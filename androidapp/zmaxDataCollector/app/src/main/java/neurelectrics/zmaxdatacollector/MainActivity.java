package neurelectrics.zmaxdatacollector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    fitbitServer server;
    String fitbitStatus="";

    int getWordAt(String[] data,int position) { //get the word (two bytes) from the zMax hex data stream and combine them to make an int
        int data1 = (int) Long.parseLong(data[position], 16); //first two digits are EEG channel 1
        int data2 = (int) Long.parseLong(data[position+1], 16);
        byte d1b = (byte) data1;
        byte d2b = (byte) data2;
        int val = ((d1b & 0xff) << 8) | (d2b & 0xff); //combine two bytes to get an int
        return val;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context cont = this;
        setContentView(R.layout.activity_main);
        final Button startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);
                DataHandler DataHandlerTask = new DataHandler();
                DataHandlerTask.execute();

                //start the Fitbit server
                 server = new fitbitServer();
                try {
                    server.start();
                } catch(IOException ioe) {
                    Log.w("Httpd", "The server could not start.");
                }
                Log.w("Httpd", "Web server initialized.");

            }
        });

        Button stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    //stop the server when app is closed
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (server != null)
            server.stop();
    }



    //fitbitServer handles getting data from the fitbit which sends it on port 8085
    private class fitbitServer extends NanoHTTPD {
        PrintWriter fitbitWriter;
        public fitbitServer() {
            super(8085);
        }


        public Response serve(String uri, Method method,
                              Map<String, String> header,
                              Map<String, String> parameters,
                              Map<String, String> files) {
            String answer = "ok"; //required because the client will get confused if there is no response
            if (uri.indexOf("rawdata") > -1) { //recieved a data packet from the Fitbit, set the Fitbit status to good.

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        TextView fStatus = (TextView) findViewById(R.id.fConnectionStatus);
                        fStatus.setText("✔️ Fitbit connected");
                    }
                });

                String[] fitbitParams=parameters.toString().replace(":",",").split(","); //split up individual data vals
                fitbitStatus=System.currentTimeMillis()+","+fitbitParams[2]+","+fitbitParams[4]+","+fitbitParams[6]+","+fitbitParams[8]+","+fitbitParams[10]+","+fitbitParams[12]+","+fitbitParams[14]; //store just sensor data value, not keys
                Log.i("fitbit", fitbitStatus);
                try {
                    FileWriter fileWriter = new FileWriter(getApplicationContext().getExternalFilesDir(null) + "/fitbitdata.txt", true);
                    PrintWriter printWriter = new PrintWriter(fileWriter);
                    printWriter.println(fitbitStatus);  //New line
                    printWriter.flush();
                    printWriter.close();
                }
                catch (IOException e) {
                    Log.e("Fitbitserver","Error writing to file");
                }
            }
           // Log.i("server", parameters.toString());

            //update the Fitbit status

            return new NanoHTTPD.Response(answer);
        }
    }

    //DataHandler receives zMax data and writes it to a file
    //Note--data is stored in UNSCALED form
    //THis function is based on the matlab code at http://hypnodynecorp.com/downloads/HDConnect.m
    private class DataHandler extends AsyncTask<Void, String, Void> {
        private Socket client;
        private PrintWriter printwriter;
        private String messsage;
        private Context mContext;
        String dataBuffer = "";


        @Override
        protected Void doInBackground(Void... params) {
            Log.i("Record", "Recording started");
            try {
                //set up the data storage file
                FileWriter fileWriter = new FileWriter(getApplicationContext().getExternalFilesDir(null) + "/zmaxdata.txt", true);
                PrintWriter pw = new PrintWriter(fileWriter);


                client = new Socket("127.0.0.1", 24000); // connect to the server
                printwriter = new PrintWriter(client.getOutputStream(), true);
                printwriter.write("HELLO\n"); // write the message to output stream

                printwriter.flush();

                InputStream is = client.getInputStream();
                while (true) {
                    int c = is.read();
                    if (c != -1) {
                        byte db = (byte) c;
                        //Log.e("data","data");
                        if (db == '\n') {
                            if (dataBuffer.length() > 1) { //we have just completed a sample, now process it
                                String[] splitup = dataBuffer.split("\\.");
                                if (splitup.length > 1) { //the stuff after the period is the actual data
                                    String[] theData = splitup[1].split("-"); //split into individual hex digits
                                    int packetType = (int) Long.parseLong(theData[0], 16);
                                    if (packetType >= 1 && packetType <= 11) { //first digit specifies the type of packet this is; we only process it if it's a dat apacket
                                        int EEG_R=getWordAt(theData,1);
                                        int EEG_L=getWordAt(theData,3);
                                        int ACC_X=getWordAt(theData,5);
                                        int ACC_Y=getWordAt(theData,7);
                                        int ACC_Z=getWordAt(theData,9);
                                        int PPG = getWordAt(theData,27);
                                        int BODYTEMP=getWordAt(theData,36);
                                        int AMBIENTLIGHT=getWordAt(theData,21);
                                        int BATTERYPOWER=getWordAt(theData,23);
                                        int AMBIENTNOISE=getWordAt(theData,19);

                                        //Log.e("EEG",""+val);
                                        pw.println(System.currentTimeMillis()+","+EEG_R+","+EEG_L+","+ACC_X+","+ACC_Y+","+ACC_Z+","+PPG+","+BODYTEMP+","+AMBIENTLIGHT+","+BATTERYPOWER+","+AMBIENTNOISE); //write the EEG
                                        pw.flush();

                                        //valid packet received, so update the connection status
                                        TextView zCon = (TextView) findViewById(R.id.zConnectionStatus);
                                        publishProgress("zmaxconnected");
                                    } else {
                                        Log.i("Error", "Wrong packet type");
                                    }
                                }
                            }
                            dataBuffer = "";
                        }
                        dataBuffer = dataBuffer + (char) db;


                    }


                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) { //handles updating the UI
            if (values[0].equals("zmaxconnected")) {
                TextView zStatus = (TextView) findViewById(R.id.zConnectionStatus);
                zStatus.setText("✔️ zMax connected");
            }
        }
    }
}