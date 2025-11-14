import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class SysInfo {

    private final OperatingSystemMXBean osBean;

    public SysInfo() {
        this.osBean = (OperatingSystemMXBean)
                ManagementFactory.getOperatingSystemMXBean();
    }

    public String GetOSName() {
        return System.getProperty("os.name");
    }

    public String GetOSVersion() {
        return System.getProperty("os.version");
    }

    public long GetFreeMemory() {
        return osBean.getFreePhysicalMemorySize();
    }

    public long GetTotalMemory() {
        return osBean.getTotalPhysicalMemorySize();
    }

    public int GetProcessorCount() {
        return Runtime.getRuntime().availableProcessors();
    }
}
