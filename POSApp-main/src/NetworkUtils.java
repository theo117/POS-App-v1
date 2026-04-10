import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public final class NetworkUtils
{
    private NetworkUtils()
    {
    }

    public static List<String> getAccessibleUrls(int port)
    {
        List<String> urls = new ArrayList<>();
        urls.add("http://127.0.0.1:" + port + "/");
        urls.add("http://localhost:" + port + "/");

        try
        {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(interfaces))
            {
                if (!networkInterface.isUp() || networkInterface.isLoopback())
                {
                    continue;
                }

                for (InetAddress address : Collections.list(networkInterface.getInetAddresses()))
                {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress())
                    {
                        urls.add("http://" + address.getHostAddress() + ":" + port + "/");
                    }
                }
            }
        }
        catch (Exception ex)
        {
            // Best effort only.
        }

        return urls;
    }
}
