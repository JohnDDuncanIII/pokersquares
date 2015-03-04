package edu.gettysburg.ai;


/**
 * PokerSquaresPlayer - a simple player interface for PokerSquares.
 * Author: Todd W. Neller
 */
public interface PokerSquaresPlayer {
	/**
	 * init - initializes the player before each game
	 */
	public void init();
	
	/**
	 * getPlay - gets the current play position for a given card within the allotted number of milliseconds.
	 * Each card passed to getPlay has been drawn from the game deck.
	 * Each legal returned move will be made for the player.
	 * Thus, this method contains all information necessary to maintain current game state for the player.
	 * @param card - card just drawn.
	 * @param millisRemaining - remaining milliseconds for play in the rest of the player's game.
	 * @return a 2D int array with the chosen (row, col) position for play of the given card.
	 */
	public int[] getPlay(Card card, long millisRemaining);
}
