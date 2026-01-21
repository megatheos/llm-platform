package com.llmplatform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Load .env file and inject variables into Spring Environment
 * Supports both project root .env and jar同级目录的 .env
 */
@Slf4j
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String DOTENV_SOURCE_NAME = "dotenv";
    private static final Pattern ENV_PATTERN = Pattern.compile("^\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(.*)\\s*$");
    private static final Pattern QUOTED_PATTERN = Pattern.compile("^[\"']?(.*)[\"']?$");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, Object> dotenvProperties = new HashMap<>();

        // 1. 优先加载项目根目录的 .env（开发环境）
        Path projectRoot = findProjectRoot();
        loadDotenvFile(dotenvProperties, projectRoot);

        // 2. 其次加载 jar 包同级目录的 .env（生产环境）
        Path jarDirectory = findJarDirectory();
        if (!jarDirectory.equals(projectRoot)) {
            loadDotenvFile(dotenvProperties, jarDirectory);
        }

        if (!dotenvProperties.isEmpty()) {
            MapPropertySource propertySource = new MapPropertySource(DOTENV_SOURCE_NAME, dotenvProperties);
            environment.getPropertySources().addLast(propertySource);
            log.info("Loaded {} variables from .env file", dotenvProperties.size());
        } else {
            log.debug("No .env file found, using system environment variables");
        }
    }

    private void loadDotenvFile(Map<String, Object> properties, Path directory) {
        for (String filename : DOTENV_FILENAMES) {
            Path dotenvPath = directory.resolve(filename);
            if (Files.exists(dotenvPath)) {
                log.info("Loading .env file from: {}", dotenvPath);
                try {
                    var lines = Files.readAllLines(dotenvPath, StandardCharsets.UTF_8);
                    int loaded = 0;
                    for (String line : lines) {
                        Matcher matcher = ENV_PATTERN.matcher(line);
                        if (matcher.matches()) {
                            String key = matcher.group(1);
                            String value = matcher.group(2);

                            // 移除引号
                            Matcher quotedMatcher = QUOTED_PATTERN.matcher(value);
                            if (quotedMatcher.matches()) {
                                value = quotedMatcher.group(1);
                            }

                            // 只在没有的情况下添加（避免覆盖已加载的）
                            if (!properties.containsKey(key)) {
                                properties.put(key, value);
                                loaded++;
                            }
                        }
                    }
                    log.info("Successfully loaded {} variables from {}", loaded, dotenvPath);
                } catch (IOException e) {
                    log.warn("Failed to load .env from {}: {}", dotenvPath, e.getMessage());
                }
            }
        }
    }

    private String[] DOTENV_FILENAMES = {".env", ".env.local"};

    /**
     * 查找项目根目录（包含 .env 的目录）
     * 向上查找直到找到 .env 文件或到达文件系统根目录
     */
    private Path findProjectRoot() {
        Path current = Paths.get("").toAbsolutePath().normalize();

        // 向上查找 .env 文件
        while (current != null) {
            for (String filename : DOTENV_FILENAMES) {
                Path dotenvPath = current.resolve(filename);
                if (Files.exists(dotenvPath)) {
                    log.debug("Found .env at: {}", dotenvPath);
                    return current;
                }
            }
            if (current.getParent() == null) {
                break;
            }
            current = current.getParent();
        }

        // 如果没找到，返回当前工作目录
        log.debug("No .env file found, using current directory: {}", Paths.get("").toAbsolutePath());
        return Paths.get("").toAbsolutePath();
    }

    /**
     * 获取 jar 包所在目录
     */
    private Path findJarDirectory() {
        try {
            String jarPath = DotenvEnvironmentPostProcessor.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath();

            String path = java.net.URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name());
            Path dirPath = Paths.get(path);

            if (dirPath.toString().endsWith(".jar")) {
                return dirPath.getParent();
            }
            return dirPath;
        } catch (Exception e) {
            log.debug("Failed to get jar directory: {}", e.getMessage());
            return Paths.get("").toAbsolutePath();
        }
    }
}
