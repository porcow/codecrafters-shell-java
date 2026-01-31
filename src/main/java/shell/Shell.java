package shell;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class Shell {
    private final ShellContext context;

    public Shell() {
        this(new ShellContext());
    }

    public Shell(ShellContext context) {
        this.context = context == null ? new ShellContext() : context;
    }

    public ShellContext getContext() {
        return context;
    }

    public void evalInput(String inputString) {
        if (inputString != null && !inputString.isBlank()) {
            HistoryCommand.record(context, inputString.trim());
        }
        List<List<String>> pipelineParts = CCParser.splitPipelineTokens(inputString);
        if (pipelineParts.size() > 1) {
            evalPipeline(pipelineParts);
            return;
        }
        CCParser.ParsedLine parsed = CCParser.parseLine(context, inputString);
        eval(parsed);
    }

    public void eval(CCParser.ParsedLine parsed) {
        if (parsed == null || parsed.command() == null) {
            return;
        }

        if (parsed.redirectTokens() == null || parsed.redirectTokens().isEmpty()) {
            runCommand(parsed.command());
            return;
        }

        List<String> redirectTokens = parsed.redirectTokens();

        CommandRunner runner = resolveRunner(parsed.command());
        if (runner == null) {
            System.out.println(parsed.command().getName() + ": command not found");
            return;
        }
        boolean append = parsed.redirectType() == CCParser.RedirectType.STDOUT_APPEND
                || parsed.redirectType() == CCParser.RedirectType.STDERR_APPEND;
        try (OutputStream redirectStream = openRedirectStream(parsed.command(),
                redirectTokens.get(0),
                append)) {
            if (parsed.redirectType() == CCParser.RedirectType.STDERR
                    || parsed.redirectType() == CCParser.RedirectType.STDERR_APPEND) {
                runner.runWithStreams(parsed.command(), System.in, System.out, redirectStream);
            } else {
                runner.runWithStreams(parsed.command(), System.in, redirectStream, System.err);
            }
        } catch (RuntimeException e) {
            reportRunError(parsed.command(), e);
        } catch (Exception e) {
            reportRunError(parsed.command(), new RuntimeException(e.getMessage(), e));
        }
    }

    public void evalPipeline(List<List<String>> parts) {
        if (parts == null || parts.isEmpty()) {
            return;
        }

        List<Command> commands = new ArrayList<>();
        List<CommandRunner> runners = new ArrayList<>();
        for (List<String> part : parts) {
            if (part == null || part.isEmpty()) {
                return;
            }
            Command command = CCParser.parseTokens(context, part);
            CommandRunner runner = resolveRunner(command);
            if (runner == null) {
                System.out.println(command.getName() + ": command not found");
                return;
            }
            commands.add(command);
            runners.add(runner);
        }

        boolean allExternal = true;
        for (Command command : commands) {
            if (command.isBuiltin()) {
                allExternal = false;
                break;
            }
        }
        if (allExternal) {
            // For external-only pipelines, use OS-level piping for correct stream behavior.
            evalExternalPipeline(commands);
            return;
        }

        List<Thread> threads = new ArrayList<>();
        InputStream nextInput = System.in;
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            CommandRunner runner = runners.get(i);
            InputStream input = nextInput;
            OutputStream output = System.out;
            if (i < commands.size() - 1) {
                try {
                    PipedOutputStream pipeOut = new PipedOutputStream();
                    PipedInputStream pipeIn = new PipedInputStream(pipeOut);
                    output = pipeOut;
                    nextInput = pipeIn;
                } catch (Exception e) {
                    reportRunError(command, new RuntimeException(e.getMessage(), e));
                    return;
                }
            }

            OutputStream targetOut = output;
            InputStream targetIn = input;
            Thread thread = new Thread(() -> {
                try {
                    // Execute each stage in a thread and wire its output to the next stage.
                    runner.runWithStreams(command, targetIn, targetOut, System.err);
                } catch (RuntimeException e) {
                    reportRunError(command, e);
                } finally {
                    if (targetOut != System.out) {
                        try {
                            targetOut.close();
                        } catch (Exception e) {
                            // Ignore close failures.
                        }
                    }
                }
            });
            threads.add(thread);
            thread.start();
        }

        Thread last = threads.get(threads.size() - 1);
        try {
            // Wait for the last stage before interrupting upstream stages.
            last.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        for (int i = 0; i < threads.size() - 1; i++) {
            Thread thread = threads.get(i);
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void evalExternalPipeline(List<Command> commands) {
        boolean interactive = System.console() != null;
        List<ProcessBuilder> builders = new ArrayList<>();
        for (int i = 0; i < commands.size(); i++) {
            Command command = commands.get(i);
            List<String> commandLine = command.toCommandLine();

            String workspace = command.getWorkspace();
            if (workspace == null || workspace.isBlank()) {
                workspace = System.getProperty("user.dir");
            }
            ProcessBuilder builder = new ProcessBuilder(commandLine)
                    .directory(new File(workspace))
                    .redirectError(ProcessBuilder.Redirect.INHERIT);
            if (i == 0) {
                // Let the first process read from our stdin.
                builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
            }
            if (i == commands.size() - 1) {
                if (interactive) {
                    // In interactive mode, inherit stdout so line-buffered tools flush immediately.
                    builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                } else {
                    // Pipe the last stdout into Java so tests can capture it.
                    builder.redirectOutput(ProcessBuilder.Redirect.PIPE);
                }
            }
            builders.add(builder);
        }

        Thread stdoutThread = null;
        List<Process> processes = null;
        try {
            // Let the OS wire the pipeline between all external commands.
            processes = ProcessBuilder.startPipeline(builders);
            Process last = processes.get(processes.size() - 1);
            if (!interactive) {
                stdoutThread = new Thread(() -> {
                    try {
                        // Stream the last process output into System.out.
                        last.getInputStream().transferTo(System.out);
                    } catch (java.io.IOException e) {
                        // Ignore pipe failures while the last process is ending.
                    } finally {
                        try {
                            System.out.flush();
                        } catch (Exception e) {
                            // Ignore flush failures.
                        }
                    }
                });
                stdoutThread.start();
            }

            // Wait for the last stage, then tear down earlier stages (e.g., tail -f).
            last.waitFor();
            if (stdoutThread != null) {
                stdoutThread.join();
            }

            for (int i = 0; i < processes.size() - 1; i++) {
                Process process = processes.get(i);
                if (process.isAlive()) {
                    process.destroy();
                }
            }
            for (Process process : processes) {
                process.waitFor();
            }
        } catch (Exception e) {
            if (stdoutThread != null && stdoutThread.isAlive()) {
                stdoutThread.interrupt();
            }
            if (processes != null) {
                for (Process process : processes) {
                    process.destroy();
                }
            }
            if (!commands.isEmpty()) {
                reportRunError(commands.get(0), new RuntimeException(e.getMessage(), e));
            }
        }
    }

    public void runCommand(Command command) {
        CommandRunner runner = resolveRunner(command);
        if (runner == null) {
            System.out.println(command.getName() + ": command not found");
            return;
        }
        try {
            runner.run(command);
        } catch (RuntimeException e) {
            reportRunError(command, e);
        }
    }

    private CommandRunner resolveRunner(Command command) {
        if (command == null || command.getName() == null || command.getName().isBlank()) {
            return null;
        }
        if (command.isBuiltin()) {
            return CommandResolver.getBuiltinMap().get(command.getName());
        }
        if (command.isRunable()) {
            return ExternalCommand.getInstance();
        }
        return null;
    }

    private void reportRunError(Command command, RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.toString();
        }

        System.err.println(command.getName() + ": " + message);
    }

    private OutputStream openRedirectStream(Command command,
                                            String redirectPath,
                                            boolean append) {
        try {
            Path path = Path.of(redirectPath);
            if (!path.isAbsolute()) {
                String base = command.getWorkspace();
                if (base == null || base.isBlank()) {
                    base = System.getProperty("user.dir");
                }
                path = Path.of(base).resolve(redirectPath);
            }
            if (append) {
                return Files.newOutputStream(path.normalize(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } else {
                return Files.newOutputStream(path.normalize(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
