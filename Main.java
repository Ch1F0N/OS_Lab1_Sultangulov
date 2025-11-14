public class Main {
    public static void main(String[] args) {
        SysInfo info = new SysInfo();

        System.out.println("OS: " + info.GetOSName());
        System.out.println("Version: " + info.GetOSVersion());
        System.out.println("Processors: " + info.GetProcessorCount());
        System.out.println("RAM: "
                + info.GetFreeMemory() / (1024*1024) + "MB free / "
                + info.GetTotalMemory() / (1024*1024) + "MB total");
    }
}
