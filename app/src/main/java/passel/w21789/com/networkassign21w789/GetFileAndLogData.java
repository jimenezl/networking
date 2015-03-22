package passel.w21789.com.networkassign21w789;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class GetFileAndLogData extends IntentService {
    private static final int timePerLog = 100; //in ms, time before taking a reading and logging it

    public GetFileAndLogData() {
        super("GetFileAndLogData");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //write to log file
    public void appendLog(String text) {
        File externalStorageDir = Environment.getExternalStorageDirectory();
        File logFile = new File(externalStorageDir, "network_log.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private void sendMessage(String message) {
//        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("send-network-data");
        // You can also include some extra data.
        intent.putExtra("message", message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onHandleIntent(Intent intent) {

        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();

        //these next five lines are only so we get a bit of extra network info
        ConnectivityManager cm =
                (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        String networkType = activeNetwork.getTypeName();
        TelephonyManager telManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);

        long currentTime= System.currentTimeMillis();

        sendMessage(convertDate(currentTime));  // send to the main Activity so user can see
        appendLog(convertDate(currentTime));

        String message = "network: " + networkType + ", " + Integer.valueOf(telManager.getNetworkType());
        sendMessage(message);  // send to the main Activity so user can see
        appendLog(message);

        long prevTime = currentTime;
        long startTime = currentTime;
        String urlToDownload = "http://web.mit.edu/21w.789/www/papers/griswold2004.pdf";
        try {
            URL url = new URL(urlToDownload);
            URLConnection connection = url.openConnection();
//            connection.connect();

            // download the file
            InputStream input = new BufferedInputStream(connection.getInputStream());

            byte data[] = new byte[1024];
            long total = 0;
            long currentTotal = 0; //used for 1 second intervals
            int count;

            //measuring latency
            input.read(data);
            currentTime = System.currentTimeMillis();
            message = "Latency: " + String.valueOf(currentTime-prevTime);
            sendMessage(message);  // send to the main Activity so user can see
            appendLog(message);
            prevTime = currentTime;

            while ((count = input.read(data)) != -1) {
                total += count;
                currentTotal += count;
                currentTime = System.currentTimeMillis();
                if ((currentTime - prevTime) >= timePerLog) { //in ms
                    message = String.valueOf(8*currentTotal/1000) + " kbps"; //8 bits per byte, 1000 bits per kbit
                    sendMessage(message);  // send to the main Activity so user can see
                    appendLog(message);
//                    Log.d("data: ", message);
                    prevTime = currentTime;
                    currentTotal = 0;
                }
            }
            input.close();
            message = "Download finished in: " + String.valueOf(currentTime - startTime) + "ms";
            sendMessage(message);  // send to the main Activity so user can see
            appendLog(message);

        } catch (IOException e) {
            e.printStackTrace();
        }



        return;
    }

    //easy date conversion
    public static String convertDate(Long dateInMilliseconds) {
        return DateFormat.format("MM/dd/yyyy hh:mm:ss", dateInMilliseconds).toString();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
    }
}
