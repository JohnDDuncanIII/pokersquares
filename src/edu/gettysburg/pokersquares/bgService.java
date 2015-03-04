package edu.gettysburg.pokersquares;

import java.util.ArrayList;
import java.util.Stack;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import edu.gettysburg.ai.*;
import android.widget.Toast;

public class bgService extends Service{
	//newPokerSquares computer;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		//AAFinalPokerSquarePlayer.start();
		
		//new Thread(new Runnable() {
		//public void run() {
		//Thread.yield();

		
		
		//START AI
		
	
		//computer.nextMove();
		//}}).start();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		//Toast.makeText(this, "My Service Started", Toast.LENGTH_LONG).show();
		Toast.makeText(this, "AI Initialized!", Toast.LENGTH_LONG).show();
		
		//Bundle bundle = intent.getExtras();
		//System.out.println(bundle.get("deck"));
		
		// what the ???? the Stack gets converted into an ArrayList....
		@SuppressWarnings("unchecked")
		ArrayList<Card> tmp = (ArrayList<Card>) intent.getSerializableExtra("deck");
		System.out.println(tmp);
		Stack<Card> deck = new Stack<Card>();
		deck.addAll(tmp);
		/*
		if(tmp instanceof ArrayList){
			System.out.println("ARRAY LIST");
		}
		if(tmp instanceof Stack){
			System.out.println("STACK");
		}
		*/
		//Stack<Card> deck = (Stack<Card>) bundle.getSerializable("deck");
		
		/*Stack<Card> deckCopy = (Stack<Card>) deck.clone();
		NARLPokerSquaresPlayer player = new NARLPokerSquaresPlayer();
		computer = new newPokerSquares(player, 60000, edu.gettysburg.ai.Card.interpret(deckCopy));
		
		
		computer.nextMove();*/
		
		/*LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		ImageView layout = (ImageView) inflater.inflate(R.id.r1c1, null);
		layout.setOnClickListener(new View.OnClickListener() {
		    @Override
		    public void onClick(View v) {
		        computer.nextMove();
		    }
		});*/
	}

	@Override
	public void onDestroy() {
		Toast.makeText(this, "MyService Stopped", Toast.LENGTH_LONG).show();
	}
	
	
}