package goldgamebot;

public class Player {
	
	private String playerID;
	private int result;
	
	public Player(String playerID){
		this.playerID = playerID;
		this.result = 0;
	}

	public String getPlayerID() {
		return playerID;
	}

	public void setPlayerID(String playerID) {
		this.playerID = playerID;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}

}
