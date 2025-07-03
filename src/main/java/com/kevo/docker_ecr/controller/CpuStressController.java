package com.kevo.docker_ecr.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@Controller
@RequestMapping("/stress-test")
public class CpuStressController {

    private volatile boolean isStressing = false;
    private volatile int currentTarget = 60;
    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );
    private final Runtime runtime = Runtime.getRuntime();
    public static final String stressTestRedirect = "redirect:/stress-test";
    public static final int instanceId = (int) (Math.random() * 10) + 1;

    @GetMapping
    public String showStressTestPage(Model model) {
        addSystemInfoToModel(model);
        return "stress-test";
    }

    @PostMapping("/start")
    public String startStress(@RequestParam(defaultValue = "60") int targetCpuPercent,
                              RedirectAttributes redirectAttributes) {

        if (isStressing) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Stress test is already running. Please stop it first.");
            return stressTestRedirect;
        }

        if (targetCpuPercent < 10 || targetCpuPercent > 95) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Target CPU percentage must be between 10% and 95%.");
            return stressTestRedirect;
        }

        currentTarget = targetCpuPercent;
        isStressing = true;

        int numThreads = runtime.availableProcessors();

        // Calculate work vs sleep ratio to achieve target CPU usage
        long sleepTime = 100L - targetCpuPercent; // milliseconds of sleep

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                while (isStressing) {
                    long startTime = System.currentTimeMillis();

                    // CPU-intensive work
                    while (System.currentTimeMillis() - startTime < (long) targetCpuPercent) {
                        // Perform meaningless calculations
                        Math.sin(Math.random() * Math.PI);
                        Math.sqrt(Math.random() * 1000);
                        Math.cos(Math.random() * Math.PI);

                        // String operations for variety
                        String dummy = "stress-test-" + Math.random();
                        dummy.toUpperCase().toLowerCase().trim();
                    }

                    // Sleep to control CPU usage
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        redirectAttributes.addFlashAttribute("successMessage",
                "CPU stress test started successfully with target: " + targetCpuPercent + "%");

        return stressTestRedirect;
    }

    @PostMapping("/stop")
    public String stopStress(RedirectAttributes redirectAttributes) {
        if (!isStressing) {
            redirectAttributes.addFlashAttribute("warningMessage",
                    "No stress test is currently running.");
            return stressTestRedirect;
        }

        isStressing = false;
        currentTarget = 0;

        redirectAttributes.addFlashAttribute("successMessage",
                "CPU stress test stopped successfully.");

        return stressTestRedirect;
    }

    @GetMapping("/status")
    @ResponseBody
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isStressing", isStressing);
        status.put("currentTarget", currentTarget);
        status.put("availableProcessors", runtime.availableProcessors());
        status.put("maxMemory", runtime.maxMemory() / (1024 * 1024)); // MB
        status.put("freeMemory", runtime.freeMemory() / (1024 * 1024)); // MB
        status.put("totalMemory", runtime.totalMemory() / (1024 * 1024)); // MB
        return status;
    }

    private void addSystemInfoToModel(Model model) {
        model.addAttribute("isStressing", isStressing);
        model.addAttribute("currentTarget", currentTarget);
        model.addAttribute("availableProcessors", runtime.availableProcessors());
        model.addAttribute("maxMemory", runtime.maxMemory() / (1024 * 1024)); // MB
        model.addAttribute("freeMemory", runtime.freeMemory() / (1024 * 1024)); // MB
        model.addAttribute("totalMemory", runtime.totalMemory() / (1024 * 1024)); // MB
        model.addAttribute("instanceId", instanceId);
    }

    @PreDestroy
    public void shutdown() {
        isStressing = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}