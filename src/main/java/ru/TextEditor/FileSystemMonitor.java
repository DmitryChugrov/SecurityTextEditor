package ru.TextEditor;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.RecursiveTask;
import javax.imageio.ImageIO;
import javax.swing.*;

public class FileSystemMonitor implements Runnable {
    private final Set<String> processedFiles = new HashSet<>();
    private final List<String> directoriesToMonitor;

    public FileSystemMonitor() {
        this.directoriesToMonitor = getAllDirectories();
    }

    @Override
    public void run() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            for (String dir : directoriesToMonitor) {
                Path path = Paths.get(dir);
                if (Files.exists(path) && Files.isDirectory(path)) {
                    try {
                        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
                    } catch (IOException e) {
//                        System.err.println("Не удалось зарегистрировать директорию для мониторинга: " + dir);
                        e.printStackTrace();
                    }
                } else {
//                    System.err.println("Директория не существует или не является директорией: " + dir);
                }
            }

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watchService.poll(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                if (key != null) {
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        Path fileName = ev.context();
                        File file = ((Path) key.watchable()).resolve(fileName).toFile();

                        if (file.exists() && !file.isDirectory() && !processedFiles.contains(file.getAbsolutePath())) {
                            if (isImageFile(file)) {
//                                System.out.println("Найден новый скриншот: " + file.getAbsolutePath());
                                boolean success = applyBlurToFile(file);
                                if (success) {
                                    processedFiles.add(file.getAbsolutePath());
                                } else {
                                    if (!file.delete()) {
//                                        System.err.println("Не удалось удалить файл после неудачных попыток: " + file.getAbsolutePath());
                                    }
                                }
                            } else if (isScreenRecordingFile(file)) {
                                JOptionPane.showMessageDialog(null,"Найден файл записи экрана, удаление: " + file.getAbsolutePath());
                                if (!file.delete()) {
//                                    System.err.println("Не удалось удалить файл записи экрана: " + file.getAbsolutePath());
                                }
                            }
                        }
                    }
                    key.reset();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isImageFile(File file) {
        String[] imageExtensions = {"png", "jpg", "jpeg", "bmp", "gif"};
        String fileName = file.getName().toLowerCase();
        for (String ext : imageExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean isScreenRecordingFile(File file) {
        String[] recordingExtensions = {"mp4", "avi", "mov", "mkv", "flv"};
        String fileName = file.getName().toLowerCase();
        for (String ext : recordingExtensions) {
            if (fileName.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private boolean applyBlurToFile(File file) {
        int maxAttempts = 3;
        int attempt = 0;
        while (attempt < maxAttempts) {
            try {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
//                    throw new IOException("ImageIO.read вернул null для файла: " + file.getAbsolutePath());
                }
                BufferedImage blurredImage = blurImage(image);
                ImageIO.write(blurredImage, "png", file);
                JOptionPane.showMessageDialog(null,"Применено замыливание к: " + file.getAbsolutePath());
                return true;
            } catch (IOException e) {
//                System.err.println("Не удалось прочитать файл как изображение: " + file.getAbsolutePath());
                e.printStackTrace();
                attempt++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
//        System.err.println("Не удалось обработать файл после нескольких попыток: " + file.getAbsolutePath());
        return false;
    }

    private BufferedImage blurImage(BufferedImage source) {
        BufferedImage dest = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(source, 0, 0, null);
        g2.setComposite(AlphaComposite.SrcOver);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float[] kernel = new float[49];
        for (int i = 0; i < 49; i++) {
            kernel[i] = 1f / 49f;
        }
        ConvolveOp convolveOp = new ConvolveOp(new Kernel(7, 7, kernel), ConvolveOp.EDGE_NO_OP, null);
        g2.drawImage(source, convolveOp, 0, 0);
        g2.dispose();
        return dest;
    }

    private List<String> getAllDirectories() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        List<String> directories = new ArrayList<>();
        File[] roots = File.listRoots();
        List<RecursiveTask<List<String>>> tasks = new ArrayList<>();
        for (File root : roots) {
            DirectoryTask task = new DirectoryTask(root);
            tasks.add(task);
            forkJoinPool.execute(task);
        }
        for (RecursiveTask<List<String>> task : tasks) {
            directories.addAll(task.join());
        }
        return directories;
    }

    private class DirectoryTask extends RecursiveTask<List<String>> {
        private final File dir;

        public DirectoryTask(File dir) {
            this.dir = dir;
        }

        @Override
        protected List<String> compute() {
            List<String> directories = new ArrayList<>();
            List<DirectoryTask> subTasks = new ArrayList<>();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        directories.add(file.getAbsolutePath());
                        DirectoryTask task = new DirectoryTask(file);
                        task.fork();
                        subTasks.add(task);
                    }
                }
                for (DirectoryTask task : subTasks) {
                    directories.addAll(task.join());
                }
            }
            return directories;
        }
    }
}


