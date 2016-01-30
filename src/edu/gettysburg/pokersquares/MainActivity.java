package edu.gettysburg.pokersquares;

/*
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
 */
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
/*
import android.content.res.AssetManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.LinearLayout;
 */
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.gettysburg.ai.*;

public class MainActivity extends Activity implements View.OnClickListener, OnTouchListener {
	private Stack<Card> 				deck; 
	private ArrayList<List<Card>> 		places  = new ArrayList<List<Card>>();
	private HashMap<String, ImageView>  map    	= new HashMap<String, ImageView>();
	private HashMap<String, ImageView>  computerMap    	= new HashMap<String, ImageView>();
	private HashMap<String, TextView>   textMap = new HashMap<String, TextView>();
	private TextView					textTotal, textTotalString;
	private ImageView  					deckView;
	private Card 			  			currentDeckCard;
	private Card[][] 					array   = new Card[5][5];
	private int 						moves   = 0, gameTotal = 0;//, highestScore;
	private boolean 					isMuted = false;
	private MediaPlayer 				mp      = new MediaPlayer();
	private String						userName;
	newPokerSquares computer;

	// swipe stuff
	private static final int NONE = 0;
	private static final int SWIPE = 1;
	private int mode = NONE;
	private float startY;
	private float stopY;
	// We will only detect a swipe if the difference is at least 100 pixels
	// Change this value to your needs
	private static final int TRESHOLD = 100;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//System.out.println("Working Directory = " + System.getProperty("user.dir"));

		// Get userName from SplashScreen activity
		Bundle bundle = getIntent().getExtras();
		userName = bundle.getString("userName");

		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/ProFontWindows.ttf");
		// Get resources for all of the vertical + horizontal textViews, then add it to the hashMap of textViews using its ID as the key
		for(int i=0; i<=9; i++) {
			int resourceID 	= getResources().getIdentifier("view"+i, "id", getPackageName());
			TextView toAdd = (TextView) findViewById(resourceID);
			if(i>=5) toAdd.setWidth(70);
			toAdd.getPaint().setAntiAlias(false);
			toAdd.setTypeface(tf);
			textMap.put("view"+i, toAdd);
		}

		View tmp = (View) findViewById (getResources().getIdentifier("linearlayout0", "id", getPackageName()));
		tmp.setPadding(75, 0, 0, 0);

		textTotal 		 = (TextView) findViewById(R.id.textTotal);
		textTotal.setTypeface(tf);
		textTotalString  = (TextView) findViewById(R.id.textTotalString);

		// Initialize card deck, then shuffle it (arbitrarily) three times to ensure randomness
		deck 			 = Card.initialize();

		Collections.shuffle(deck);

		@SuppressWarnings("unchecked")
		Stack<Card> deckCopy = (Stack<Card>) deck.clone();
		NARLPokerSquaresPlayer player = new NARLPokerSquaresPlayer();
		computer = new newPokerSquares(player, 60000, edu.gettysburg.ai.Card.interpret(deckCopy));

		//startService(new Intent(this, bgService.class).putExtra("deck", deck));

		// For clarity on colored backgrounds...
		textTotal.setShadowLayer(7, 0, 0, Color.BLACK);
		textTotalString.setShadowLayer(7, 0, 0, Color.BLACK);

		// so we can initialize the card faces with a cool pattern
		int counter = 2;
		// Get resources for all of the ImageViews, set their onClickListener, 
		//    and then add them to them hashMap of ImageViews using its ID as the key
		for(int r=1; r<6; r++) {
			for(int c=1; c<6; c++) {
				int resourceID 	= getResources().getIdentifier("r" + r + "c" + c, "id", getPackageName());
				ImageView toAdd = (ImageView) findViewById(resourceID);
				toAdd.setOnClickListener(this);
				map.put("r" + r + "c" + c, toAdd);

				// dope ternary - you = jealous 
				Bitmap initialBmp = counter % 2 == 0 ? BitmapFactory.decodeResource(this.getResources(), R.drawable.topbvert): 
					BitmapFactory.decodeResource(this.getResources(), R.drawable.toprvert);

				initialBmp = Bitmap.createScaledBitmap(initialBmp, initialBmp.getWidth(), initialBmp.getHeight(), false); 
				BitmapDrawable initialCur = new BitmapDrawable(this.getResources(), initialBmp);
				initialCur.setAntiAlias(false);
				toAdd.getLayoutParams().height = initialBmp.getHeight();
				toAdd.getLayoutParams().width = initialBmp.getWidth();
				toAdd.requestLayout();

				toAdd.setImageDrawable(initialCur);
				counter++;
			}
		}

		// Get resource for the deckView, then pop the first card off of the stack and set the top of the deckView equal to the cards resource in /res
		deckView 		= (ImageView) findViewById(R.id.deckView);
		currentDeckCard = deck.pop();
		String fileName = currentDeckCard.toString();
		fileName		= fileName.toLowerCase(Locale.getDefault());
		int resourceID  = getResources().getIdentifier(fileName, "drawable", getPackageName());

		Bitmap deckBmp = BitmapFactory.decodeResource(this.getResources(), resourceID);
		deckBmp = Bitmap.createScaledBitmap(deckBmp, deckBmp.getWidth(), deckBmp.getHeight(), false); 
		BitmapDrawable deckCur = new BitmapDrawable(this.getResources(), deckBmp);
		deckCur.setAntiAlias(false);

		deckView.getLayoutParams().height = deckBmp.getHeight();
		deckView.getLayoutParams().width = deckBmp.getWidth();
		deckView.requestLayout();
		deckView.setImageDrawable(deckCur);
	}

	/**
	 * The method that is called each time any ImageView is pressed in the program. 
	 */
	@Override
	public void onClick(View v) {
		// System.out.println("PX :" + dpFromPx(this, 71));
		// System.out.println("PX :" + dpFromPx(this, 96));
		// increment computer move by one
		computer.nextMove();
		// play simple sound when placing card on the table. short and succinct
		playPlace();
		// Set the currentView equal to the currently pressed ImageView
		ImageView currentView = map.get(getResources().getResourceEntryName(v.getId()));
		String fileName 	  = currentDeckCard.toString();
		fileName 			  = fileName.toLowerCase(Locale.getDefault());
		int resourceID 	      = getResources().getIdentifier(fileName, "drawable", getPackageName());

		// Get the coordinates of the view from the name, then add it to the master array of cards for computation purposes
		String imageViewName  = getResources().getResourceEntryName(v.getId());
		int row 			  = Integer.parseInt(imageViewName.substring(1,2)) - 1;
		int col 			  = Integer.parseInt(imageViewName.substring(3,4)) - 1;
		array[row][col]		  = currentDeckCard;

		// since we are using Bitmap playing cards (thanks Susan Kare), 
		//    we have to ensure that they will NOT be anti-aliased
		Bitmap gridBmp = BitmapFactory.decodeResource(this.getResources(), resourceID);
		gridBmp = Bitmap.createScaledBitmap(gridBmp, gridBmp.getWidth(), gridBmp.getHeight(), false); 

		//currentView.setImageBitmap(gridBmp);
		// Set the actual image of the ImageView in the program to the resource
		//currentView.setImageResource(resourceID);

		BitmapDrawable bdCur = new BitmapDrawable(this.getResources(), gridBmp);
		bdCur.setAntiAlias(false);

		currentView.getLayoutParams().height = gridBmp.getHeight();
		currentView.getLayoutParams().width = gridBmp.getWidth();
		currentView.requestLayout();

		currentView.setImageDrawable(bdCur);
		currentView.setClickable(false);


		// Get the next card in the deck and make it the next card in the deckView
		currentDeckCard 	  = deck.pop();
		fileName 			  = currentDeckCard.toString();
		fileName 			  = fileName.toLowerCase(Locale.getDefault());
		resourceID  		  = getResources().getIdentifier(fileName, "drawable", getPackageName());

		Bitmap bmp = BitmapFactory.decodeResource(this.getResources(), resourceID);
		bmp = Bitmap.createScaledBitmap(bmp, bmp.getWidth(), bmp.getHeight(), false); 
		BitmapDrawable bd = new BitmapDrawable(this.getResources(), bmp);
		bd.setAntiAlias(false);

		deckView.getLayoutParams().height = bmp.getHeight();
		deckView.getLayoutParams().width = bmp.getWidth();
		deckView.requestLayout();
		deckView.setImageDrawable(bd);

		moves++;
		updateArray();
		checkScoreUpdateLabels();
		updateTotal();

		//System.out.println("SHOWING COMPUTER GRID ITSELF");
		showAI();

		// When the game ends...
		if(moves==25) {
			endGame();
		}
	}

	/**
	 * Uses a temporary List to gather elements from the master array[][]  
	 * Puts List into the master ArrayList of Lists in order for computation purposes for the scoring
	 */
	private void updateArray(){
		List<Card> tmp = new LinkedList<Card>();
		places = new ArrayList<List<Card>>(); 

		// Get [rows][cols]
		for (int r=0; r<array.length; r++) {
			for(int c=0; c<array[r].length; c++) {
				if(array[r][c]!=null)
					// If the value exists, add it to a temporary list for the row/col
					tmp.add(array[r][c]);
			}
			places.add(tmp);
			tmp = new LinkedList<Card>();
		}

		tmp = new LinkedList<Card>();

		// Get [cols][rows]
		for (int r=0; r<array.length; r++) {
			for(int c=0; c<array[r].length; c++) {
				if(array[c][r]!=null)
					tmp.add(array[c][r]);
			}
			places.add(tmp);
			tmp = new LinkedList<Card>();
		}

		// Sort all of the temporary lists (since our Card class implements Comparable)
		for (int i=0; i<places.size(); i++) {
			Collections.sort(places.get(i));
		}

		/* 
		 * // DEBUG: Output the text representation of the sorted card arrays. For debug purposes -- Delete before final release.
		for(int i=0; i<places.size(); i++) {
			for(int k=0; k<places.get(i).size(); k++) {
				System.out.print(places.get(i).get(k) + " ");
			}
			System.out.println();
		}
		 */
	}

	/**
	 * When the game ends, update the array and check the score one final time and then update the total.
	 * Then show an alertDialog allowing the user to either continue playing or exit the game.
	 */
	public void endGame() {
		updateArray();
		checkScoreUpdateLabels();
		deckView.setImageResource(getResources().getIdentifier("nblank", "drawable", getPackageName()));
		updateTotal();

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Game Over! \nCongratulations " + userName + "!" + " \nTotal score was " + textTotal.getText())       
		.setCancelable(false)
		.setPositiveButton("New Game", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// Finish the current Intent, and start over
				Intent intent = getIntent();
				killAI();
				finish();
				startActivity(intent);
			}
		})
		.setNegativeButton("Quit", new DialogInterface.OnClickListener() {           
			public void onClick(DialogInterface dialog, int id) {                
				// Proper way of ending Intent
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				MainActivity.this.finish();
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	private void killAI() {
		stopService(new Intent(this, bgService.class));
	}

	/**
	 * Play the place sound when a user clicks on a click-able ImageView. 
	 * For Reference: Sound defined in res/raw/cardplace.wav
	 */
	private void playPlace() {
		new Thread(new Runnable() {
			public void run() {
				Thread.yield();
				mp = MediaPlayer.create(MainActivity.this, R.raw.cardplace);

				if(mp == null) {            
					System.out.println("Create() on MediaPlayer failed.");       
				} else if(!isMuted) {
					mp.setOnCompletionListener(new OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mediaplayer) {
							mediaplayer.stop();
							mediaplayer.release();
						}});
					mp.start();
				}}}).start();
	}

	/**
	 * Check the scoring value of each of the Lists in the places ArrayList instance variable. 
	 * Could easily implement a British scoring system option in the future...
	 */
	public void checkScoreUpdateLabels() {
		for(int i=0; i<places.size(); i++) {
			List<Card> tmp = places.get(i);
			int sectionTotal = 0;

			if(tmp.size()==5 && Card.isRoyalFlush(tmp)) {
				sectionTotal+=100;
			}
			else if(tmp.size()==5 && Card.isStraightFlush(tmp)) {
				sectionTotal+=75;
			}
			else if(tmp.size()>=4 && Card.isFourOfAKind(tmp)) {
				sectionTotal+=50;
			}
			else if(tmp.size()==5 && Card.isFullHouse(tmp)) {
				sectionTotal+=25;
			}
			else if(tmp.size()==5 && Card.isFlush(tmp)) {
				sectionTotal+=20;
			}
			else if(tmp.size()==5 && Card.isStraight(tmp)) {
				sectionTotal+=15;
			}
			else if(tmp.size()>=3 && Card.hasThreeOfAKind(tmp)) {
				sectionTotal+=10;
			}
			else if(tmp.size()>=4 && Card.hasTwoPair(tmp, tmp.size())) {
				sectionTotal+=5;
			}
			else if(tmp.size()>=2 && Card.hasPair(tmp)) {
				sectionTotal+=2;
			}

			textMap.get("view"+ i).setText(String.valueOf(sectionTotal));
		}
	}

	/**
	 * Scrape the values of each of the views and add them all together for the total.
	 */
	public void updateTotal() {
		int total = 0;
		for(int i=0; i<places.size(); i++) {
			total+=Integer.parseInt(String.valueOf(textMap.get("view"+ i).getText()));
		}
		gameTotal = total;
		textTotal.setText(String.valueOf(gameTotal));
	}

	/**
	 * When the user presses the physical back button on their device, properly finish the Intent and exit the application
	 */
	@Override
	public void onBackPressed() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_HOME);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
		killAI();
		finish();
	}

	/**
	 * Save instance variables for when the application is tilted to either landscape or portrait mode 
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/**
	 * Restore instance variables for when the application is tilted to either landscape or portrait mode 
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	/**
	 * Method to determine functionality of the menu items when they are pressed. 
	 * For Reference: Menu items defined in res/menu/my_options_menu.xml
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.about:

			return true;
		case R.id.stats:

			return true;
		case R.id.mute:
			if(isMuted) {
				Toast.makeText(getApplicationContext(), "Sound Un-Muted",
						Toast.LENGTH_SHORT).show();
				item.setTitle("Mute");
				isMuted=!isMuted;
			}
			else {
				Toast.makeText(getApplicationContext(), "Sound Muted",
						Toast.LENGTH_SHORT).show();
				item.setTitle("Un-Mute");
				isMuted=!isMuted;
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean onTouch(View v, MotionEvent event)
	{
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			// This happens when you touch the screen with two fingers
			mode = SWIPE;
			// You can also use event.getY(1) or the average of the two
			startY = event.getY(0);
			break;

		case MotionEvent.ACTION_POINTER_UP:
			// This happens when you release the second finger
			mode = NONE;
			if(Math.abs(startY - stopY) > TRESHOLD) {
				if(startY > stopY) {
					System.out.println("SWIPING UP");
				}
				else {
					System.out.println("SWIPING DOWN");
				}
			}
			this.mode = NONE;
			v.performClick();
			break;

		case MotionEvent.ACTION_MOVE:
			if(mode == SWIPE) {
				stopY = event.getY(0);
			}
			break;
		}

		return true;
	}

	public boolean onTouchEvent(MotionEvent event) {
		int action = event.getAction();
		switch(action & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_POINTER_DOWN:
			// multi-touch!! - touch down
			int count = event.getPointerCount(); // Number of 'fingers' in this time
			break;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.my_options_menu, menu);
		return true;
	}

	public void showAI(){
		edu.gettysburg.ai.Card[][] grid = computer.getGrid();

		/*
		for (int j = 0; j<grid[0].length; j++){
			for (int i = 0; i<grid.length; i++){
				System.out.println(grid[j][i]);
			}
		}
		*/

		//System.out.println(grid);

		int internalCounter = 0;
		int counter = 0;
		// so we can initialize the card faces with a cool pattern
		// Get resources for all of the ImageViews, set their onClickListener, 
		//    and then add them to them hashMap of ImageViews using its ID as the key
		for(int r=1; r<6; r++) {
			internalCounter = 0;
			for(int c=1; c<6; c++) {
				int resourceID 	= getResources().getIdentifier("r" + r + "c" + c, "id", getPackageName());
				ImageView toAdd = (ImageView) findViewById(resourceID);
				toAdd.setOnClickListener(this);
				computerMap.put("r" + r + "c" + c, toAdd);

				Bitmap initialBmp = null;
				
				if(grid[counter][internalCounter] == null) {
					initialBmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.toprbvert);
				} else {
					edu.gettysburg.ai.Card ca = grid[counter][internalCounter];
					//System.out.println("RANK " + ca.getRank() + " SUIT: " + ca.getSuit());
					
					// reverse the interpret
					int newRank = ca.getRank()-1;
					if(ca.getRank()==0)
						newRank = 12;
					Card pca= new Card(Card.Rank.values()[newRank], Card.Suit.values()[ca.getSuit()]);
					//System.out.println("NEW RANK: + " + pca.rank() + " SUIT: " + pca.suit());
					
					String fileName 	  = pca.toString();
					fileName 			  = fileName.toLowerCase(Locale.getDefault());
					int nResourceID 	      = getResources().getIdentifier(fileName, "drawable", getPackageName());
					// Get the coordinates of the view from the name, then add it to the master array of cards for computation purposes
			
					
					initialBmp = BitmapFactory.decodeResource(this.getResources(), nResourceID);
				}
				

				initialBmp = Bitmap.createScaledBitmap(initialBmp, initialBmp.getWidth(), initialBmp.getHeight(), false); 
				BitmapDrawable initialCur = new BitmapDrawable(this.getResources(), initialBmp);
				initialCur.setAntiAlias(false);
				toAdd.getLayoutParams().height = initialBmp.getHeight();
				toAdd.getLayoutParams().width = initialBmp.getWidth();
				toAdd.requestLayout();

				toAdd.setImageDrawable(initialCur);
				internalCounter++;
			}
			counter++;
		}
		 

		// http://stackoverflow.com/questions/7785649/creating-a-3d-flip-animation-in-android-using-xml
		/*ObjectAnimator anim = (ObjectAnimator) AnimatorInflater.loadAnimator(this, R.animator.flipping); 
		anim.setTarget(currentView);
		anim.setDuration(1500);
		anim.start();
		 */
	}
}
