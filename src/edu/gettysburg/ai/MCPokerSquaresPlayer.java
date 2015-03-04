package edu.gettysburg.ai;


public interface MCPokerSquaresPlayer extends PokerSquaresPlayer {
	void makePlay(Card card, int row, int col);
	void undoPlay();
}
