package edu.gettysburg.pokersquares;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import edu.gettysburg.ai.*;
import android.widget.Toast;
 
public class bgService extends Service{
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		//AAFinalPokerSquarePlayer.start();
		Toast.makeText(this, "Congrats! MyService Created", Toast.LENGTH_LONG).show();
		
	}

	@Override
	public void onStart(Intent intent, int startId) {
		Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "MyService Stopped", Toast.LENGTH_LONG).show();
	}
}