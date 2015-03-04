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
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import edu.gettysburg.ai.*;

public class MainActivity extends Activity implements View.OnClickListener {
	private Stack<Card> 				deck; 
	private ArrayList<List<Card>> 		places  = new ArrayList<List<Card>>();
	private HashMap<String, ImageView> map    	= new HashMap<String, ImageView>();
	private HashMap<String, TextView>  textMap = new HashMap<String, TextView>();
	private TextView					textTotal, textTotalString;
	private ImageView  					deckView;
	private Card 			  			currentDeckCard;
	private Card[][] 					array   = new Card[5][5];
	private int 						moves   = 0, gameTotal = 0;//, highestScore;
	private boolean 					isMuted = false;
	private MediaPlayer 				mp      = new MediaPlayer();
	private String						userName;
	newPokerSquares computer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		
		
		//System.out.println("Working Directory = " + System.getProperty("user.dir"));


		// Get userName from SplashScreen activity
		Bundle bundle = getIntent().getExtras();
		userName = bundle.getString("userName");

		// Get resources for all of the vertical + horizontal textViews, then add it to the hashMap of textViews using its ID as the key
		for(int i=0; i<=9; i++) {
			int resourceID 	= getResources().getIdentifier("view"+i, "id", getPackageName());
			TextView toAdd = (TextView) findViewById(resourceID);
			textMap.put("view"+i, toAdd);
		}

		textTotal 		 = (TextView) findViewById(R.id.textTotal);
		textTotalString  = (TextView) findViewById(R.id.textTotalString);

		// Initialize card deck, then shuffle it (arbitrarily) three times to ensure randomness
		deck 			 = Card.initialize();

		Collections.shuffle(deck);
		//Collections.shuffle(deck);
		//Collections.shuffle(deck);

		@SuppressWarnings("unchecked")
		Stack<Card> deckCopy = (Stack<Card>) deck.clone();
		NARLPokerSquaresPlayer player = new NARLPokerSquaresPlayer();
		computer = new newPokerSquares(player, 60000, edu.gettysburg.ai.Card.interpret(deckCopy));

		//startService(new Intent(this, bgService.class).putExtra("deck", deck));
		
		// For clarity on colored backgrounds...
		textTotal.setShadowLayer(7, 0, 0, Color.BLACK);
		textTotalString.setShadowLayer(7, 0, 0, Color.BLACK);

		// Get resources for all of the ImageViews, set their onClickListener, and then add them to them hashMap of ImageViews using its ID as the key
		for(int r=1; r<6; r++) {
			for(int c=1; c<6; c++) {
				int resourceID 	= getResources().getIdentifier("r" + r + "c" + c, "id", getPackageName());
				ImageView toAdd = (ImageView) findViewById(resourceID);
				toAdd.setOnClickListener(this);
				map.put("r" + r + "c" + c, toAdd);
				//map.get("r" + r + "c" + c).setOnClickListener(this);
			}
		}

		// Get resource for the deckView, then pop the first card off of the stack and set the top of the deckView equal to the cards resource in /res
		deckView 		= (ImageView) findViewById(R.id.deckView);
		currentDeckCard = deck.pop();
		String fileName = currentDeckCard.toString();
		fileName		= fileName.toLowerCase(Locale.getDefault());
		int resourceID  = getResources().getIdentifier(fileName, "drawable", getPackageName());
		deckView.setImageResource(resourceID);
	}

	public void setImagesAfterRestore(String viewID) {

	}

	/**
	 * The method that is called each time any ImageView is pressed in the program. 
	 */
	@Override
	public void onClick(View v) {
		
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

		// Set the actual image of the ImageView in the program to the resource
		currentView.setImageResource(resourceID);
		currentView.setClickable(false);

		// Get the next card in the deck and make it the next card in the deckView
		currentDeckCard 	  = deck.pop();
		fileName 			  = currentDeckCard.toString();
		fileName 			  = fileName.toLowerCase(Locale.getDefault());
		resourceID  		  = getResources().getIdentifier(fileName, "drawable", getPackageName());
		deckView.setImageResource(resourceID);
		moves++;
		updateArray();
		checkScoreUpdateLabels();
		updateTotal();

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
			else if(tmp.size()>=4 && Card.hasTwoPair(tmp)) {
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

		/*
		for (HashMap.Entry<String, ImageView> entry : map.entrySet()) {
		    int id = entry.getValue().getId();
		}
		 */


		/*
		outState.putSerializable("deck", deck);
		outState.putSerializable("places", places);
		outState.putSerializable("map", map);
		outState.putSerializable("textMap", textMap);
		outState.putSerializable("textTotal", textTotal.getText().toString());
		outState.putSerializable("array", array);
		outState.putSerializable("currentDeckCard", currentDeckCard);
		outState.putInt("moves", moves);
		outState.putBoolean("isMuted", isMuted);
		outState.putString("userName", userName);
		 */

		/*
	private ImageView  					deckView;
	private MediaPlayer 				mp      = new MediaPlayer();
		 */
	}

	/**
	 * Restore instance variables for when the application is tilted to either landscape or portrait mode 
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		/*
		deck = (Stack<Card>) savedInstanceState.getSerializable("deck");
		places = (ArrayList<List<Card>>) savedInstanceState.getSerializable("places");
		map = (HashMap<String, ImageView>) savedInstanceState.getSerializable("map");
		textMap = (HashMap<String, TextView>) savedInstanceState.getSerializable("textMap");
		textTotal.setText(savedInstanceState.getString("textTotal"));
		array = (Card[][]) savedInstanceState.getSerializable("array");
		currentDeckCard = (Card) savedInstanceState.getSerializable("currentDeckCard");
		moves = savedInstanceState.getInt("moves");
		isMuted =  savedInstanceState.getBoolean("isMuted");
		userName = savedInstanceState.getString("userName");
		 */


		/*
		setNumReds(savedInstanceState.getInt("textViewNumReds", 0));
		setNumGreens(savedInstanceState.getInt("textViewNumGreens", 0));
		setUserScore(savedInstanceState.getInt("userScore", 0));
		setComputerScore(savedInstanceState.getInt("computerScore", 0));
		setTurnTotal(savedInstanceState.getInt("turnTotal", 0));
		setImage(savedInstanceState.getString("imageName"));
		setUserName(savedInstanceState.getString("userName"));
		redsDrawn=savedInstanceState.getInt("redsDrawn");
		greensDrawn=savedInstanceState.getInt("greensDrawn");
		bag=savedInstanceState.getBooleanArray("bag");
		chipIndex=savedInstanceState.getInt("chipIndex");
		editTextEnteredText.setText(savedInstanceState.getString("editTextEnteredText"));
		userStartGame = savedInstanceState.getBoolean("userStartGame", true);
		isUserTurn = savedInstanceState.getBoolean("isUserTurn", true);
		setButtonsState();
		 */
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.my_options_menu, menu);
		return true;
	}
	/*
	private void scaleImage()
	{
	    // Get the ImageView and its bitmap
	    ImageView view = (ImageView) findViewById(R.id.r1c1);
	    Drawable drawing = view.getDrawable();
	    if (drawing == null) {
	        return; // Checking for null & return, as suggested in comments
	    }
	    Bitmap bitmap = ((BitmapDrawable)drawing).getBitmap();

	    // Get current dimensions AND the desired bounding box
	    int width = bitmap.getWidth();
	    int height = bitmap.getHeight();
	    int bounding = dpToPx(250);
	    Log.i("Test", "original width = " + Integer.toString(width));
	    Log.i("Test", "original height = " + Integer.toString(height));
	    Log.i("Test", "bounding = " + Integer.toString(bounding));

	    // Determine how much to scale: the dimension requiring less scaling is
	    // closer to the its side. This way the image always stays inside your
	    // bounding box AND either x/y axis touches it.  
	    float xScale = ((float) bounding) / width;
	    float yScale = ((float) bounding) / height;
	    float scale = (xScale <= yScale) ? xScale : yScale;
	    Log.i("Test", "xScale = " + Float.toString(xScale));
	    Log.i("Test", "yScale = " + Float.toString(yScale));
	    Log.i("Test", "scale = " + Float.toString(scale));

	    // Create a matrix for the scaling and add the scaling data
	    Matrix matrix = new Matrix();
	    matrix.postScale(scale, scale);

	    // Create a new bitmap and convert it to a format understood by the ImageView 
	    Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
	    width = scaledBitmap.getWidth(); // re-use
	    height = scaledBitmap.getHeight(); // re-use
	    BitmapDrawable result = new BitmapDrawable(scaledBitmap);
	    Log.i("Test", "scaled width = " + Integer.toString(width));
	    Log.i("Test", "scaled height = " + Integer.toString(height));

	    // Apply the scaled bitmap
	    view.setImageDrawable(result);

	    // Now change ImageView's dimensions to match the scaled image
	    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams(); 
	    params.width = width;
	    params.height = height;
	    view.setLayoutParams(params);

	    Log.i("Test", "done");
	}

	private int dpToPx(int dp)
	{
	    float density = getApplicationContext().getResources().getDisplayMetrics().density;
	    return Math.round((float)dp * density);
	}
	 */
}
