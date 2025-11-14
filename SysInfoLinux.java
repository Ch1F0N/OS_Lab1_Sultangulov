import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.platform.linux.LibC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class SysInfoLinux {

    // Struct utsname for uname()
    public static class Utsname extends Structure {
        private static final int _UTSNAME_LENGTH = 65;

        public byte[] sysname = new byte[_UTSNAME_LENGTH];
        public byte[] nodename = new byte[_UTSNAME_LENGTH];
        public byte[] release = new byte[_UTSNAME_LENGTH];
        public byte[] version = new byte[_UTSNAME_LENGTH];
        public byte[] machine = new byte[_UTSNAME_LENGTH];

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(
                    "sysname",
                    "nodename",
                    "release",
                    "version",
                    "machine"
            );
        }
    }

    public interface CLib extends com.sun.jna.Library {
        CLib INSTANCE = Native.load("c", CLib.class);

        int uname(Utsname utsname);

        int gethostname(byte[] name, int len);

        int getlogin_r(byte[] buf, int bufsize);

        long get_nprocs();
    }

    public static void main(String[] args) throws Exception {

        if (!Platform.isLinux()) {
            System.out.println("This program works only on Linux.");
            return;
        }

        printOSInfo();
        printKernelAndArch();
        printUserAndHostname();
        printMemory();
        printVirtualMemory();
        printCPU();
        printLoadAvg();
        printDrives();
    }

    private static void printOSInfo() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("/etc/os-release"));
            Map<String, String> map = new HashMap<>();
            for (String line : lines) {
                if (line.contains("=")) {
                    String[] p = line.split("=", 2);
                    map.put(p[0], p[1].replace("\"", ""));
                }
            }
            System.out.println("OS: " + map.getOrDefault("PRETTY_NAME", "Unknown"));
        } catch (Exception e) {
            System.out.println("OS: Unknown");
        }
    }

    private static void printKernelAndArch() {
        Utsname u = new Utsname();
        CLib.INSTANCE.uname(u);

        String kernel = new String(u.release).trim();
        String arch = new String(u.machine).trim();

        System.out.println("Kernel: " + kernel);
        System.out.println("Architecture: " + arch);
    }

    private static void printUserAndHostname() {
        try {
            byte[] host = new byte[256];
            CLib.INSTANCE.gethostname(host, host.length);
            String hostname = new String(host).trim();

            byte[] user = new byte[256];
            int res = CLib.INSTANCE.getlogin_r(user, user.length);
            String username = (res == 0) ? new String(user).trim() : System.getProperty("user.name");

            System.out.println("Hostname: " + hostname);
            System.out.println("User: " + username);
        } catch (Exception e) {
            System.out.println("Hostname: Unknown");
            System.out.println("User: " + System.getProperty("user.name"));
        }
    }

    private static void printMemory() {
        long memTotal = 0, memFree = 0, swapTotal = 0, swapFree = 0;

        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal")) memTotal = parseKb(line);
                else if (line.startsWith("MemFree")) memFree = parseKb(line);
                else if (line.startsWith("SwapTotal")) swapTotal = parseKb(line);
                else if (line.startsWith("SwapFree")) swapFree = parseKb(line);
            }
        } catch (Exception ignored) {}

        System.out.printf("RAM: %dMB free / %dMB total%n", memFree / 1024, memTotal / 1024);
        System.out.printf("Swap: %dMB free / %dMB total%n", swapFree / 1024, swapTotal / 1024);
    }

    private static long parseKb(String line) {
        return Long.parseLong(line.replaceAll("[^0-9]", ""));
    }

    private static void printVirtualMemory() {
        long vmalloc = 0;
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("VmallocTotal")) {
                    vmalloc = parseKb(line);
                    break;
                }
            }
        } catch (Exception ignored) {}

        System.out.println("Virtual memory: " + (vmalloc / 1024) + " MB");
    }

    private static void printCPU() {
        long n = CLib.INSTANCE.get_nprocs();
        System.out.println("Processors: " + n);
    }

    private static void printLoadAvg() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/loadavg"))) {
            String[] p = br.readLine().split(" ");
            System.out.println("Load average: " + p[0] + ", " + p[1] + ", " + p[2]);
        } catch (Exception e) {
            System.out.println("Load average: Unknown");
        }
    }

    private static void printDrives() {
        System.out.println("Drives:");

        try (BufferedReader br = new BufferedReader(new FileReader("/proc/mounts"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\s+");
                if (p.length < 3) continue;

                String mount = p[1];
                String fs = p[2];

                File file = new File(mount);
                if (!file.exists()) continue;

                long total = file.getTotalSpace() / (1024L * 1024 * 1024);
                long free = file.getFreeSpace() / (1024L * 1024 * 1024);

                System.out.printf("  %-10s %-7s %dGB free / %dGB total%n",
                        mount, fs, free, total);
            }
        } catch (Exception ignored) {}
    }
}
