package goldgamebot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;

public class GoldGameBot {

	private static Properties prop;
	
	private static boolean isGameRunning = false;
	private static boolean isBetSet = false;
	private static boolean isGameClosed = false;
	private static int betAmount;
	private static List<Player> playerList;
	private static int numPlayersRolled = 0;


	public static void main(String[] args) {

		prop = new Properties();
		String propFileName = "./config.properties";

		// LOAD PROPERTIES
		try {

			File file = new File(propFileName);
			FileInputStream in = new FileInputStream(file);
			prop.load(in);
			in.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


		// GET BOT TOKEN
		final String token = prop.getProperty("token");
		final String boundChannel = prop.getProperty("bindToChannel");
		final String commandPrefix = prop.getProperty("CommandPrefix");
		

		DiscordAPI api = Javacord.getApi(token, true);
		api.setGame("GoldGameBot");

		// CONNECT BOT
		api.connect(new FutureCallback<DiscordAPI>(){
			public void onSuccess(final DiscordAPI api){

				//----------------------
				//  MAIN BOT CODE HERE
				//----------------------

				api.registerListener(new MessageCreateListener(){
					public void onMessageCreate(DiscordAPI api, Message message){

						boolean isBound = false;
						if(!boundChannel.isEmpty())
							isBound = true;

						boolean fromBoundChannel = false;
						if(message.getReceiver().getId().equalsIgnoreCase(boundChannel))
							fromBoundChannel = true;


						// START GOLD GAME
						if(isBound && fromBoundChannel && message.getContent().toUpperCase().startsWith(commandPrefix + "GOLDGAME")){

							String action = message.getContent().toUpperCase().substring(10);
							System.out.println("Action = " + action);
							
							
							int command = 0;
							
							// DETERMINE WHICH COMMAND WAS USED
							if(action.toUpperCase().contains("START")){
								command = 1;
							}else if(action.toUpperCase().contains("BET")){
								command = 2;
							}else if(action.toUpperCase().contains("ADD")){
								command = 3;
							}else if(action.toUpperCase().contains("CLOSE")){
								command = 4;
							}else if(action.toUpperCase().contains("ROLL")){
								command = 5;
							}
							
							// EXECUTE THE COMMAND
							switch(command){

							case 1: // START
								if(!isGameRunning){
									isGameRunning = true;
									message.reply("Starting a new GoldGame.  Please specify a bet amount using '"
											+ commandPrefix + "GOLDGAME BET <AMOUNT>'" );
								}else{
									message.reply("@" + message.getAuthor().getId() + " A GoldGame is already running!");
								}
								break;
							case 2: // BET
								if(isGameRunning){
									betAmount = Integer.parseInt(message.getContent().substring(14));
									message.reply("Bet amount for the current game is now set to " + betAmount + " gold.\n"
											+ "To join the current Gold Game, please use the command '" + commandPrefix + 
											"GOLDGAME ADD'" );
									isBetSet = true;
								}else{
									message.reply("@" + message.getAuthor().getId() + " No Gold Game is currently in-progress.");
								}
								break;
							case 3: // ADD
								if(isBetSet && isGameRunning && !isGameClosed){
									String playerId = message.getAuthor().getId();
									Player player = new Player(playerId);
									playerList.add(player);
									message.reply("@" + message.getAuthor().getId() + " has joined this round!");
								}else{
									if(!isBetSet)
										message.reply("Bet amount has not been set yet.  Please wait.");
									if(isGameClosed)
										message.reply("@" + message.getAuthor().getId() + " Registration for this round has already closed.");
								}
								break;
							case 4: // CLOSE
								if(isBetSet && isGameRunning && (playerList.size() >= 2) ){
									isGameClosed = true;
									message.reply("Registration for this round is now closed!  Roll using '" + commandPrefix + "GOLDGAME ROLL'");
								}else{
									message.reply("@" + message.getAuthor().getId() + " Not enough players have joined this round!");
								}
								break;
							case 5: // ROLL
								String playerID = message.getAuthor().getId();
								int result = rolled();
								
								for(int n = 0; n < playerList.size(); n++){
									if(playerList.get(n).getPlayerID().equalsIgnoreCase(playerID)){
										playerList.get(n).setResult(result);
									}
								}
								
								message.reply("@" + playerID + " has rolled " + result);
								
								Player largest = null;
								Player smallest = null;
								if(numPlayersRolled == playerList.size()){
									
									message.reply("\n\n-------------------------------------------\n\n");
									
									largest = playerList.get(0);
									smallest = playerList.get(0);
									
									for(int i = 1; i < playerList.size()-1; i++){
										
										// IN CASE OF TIE FOR LARGEST
										if(playerList.get(i).getResult() == largest.getResult()){
											
											int rand = (int)Math.random();  // Result is either 0 or 1
											
											if(rand == 1){
												largest = playerList.get(i);
											}
											
										// NEXT PLAYER HAS HIGHER RESULT	
										}else if(playerList.get(i).getResult() > largest.getResult()){
											largest = playerList.get(i);
										}
										
										
										
										// IN CASE OF TIE FOR SMALLEST
										if(playerList.get(i).getResult() == smallest.getResult()){
											int rand = (int)Math.random();  // Result is either 0 or 1
											
											if(rand == 1){
												smallest = playerList.get(i);
											}
											
										// NEXT PLAYER HAS LOWER RESULT
										}else if(playerList.get(i).getResult() < smallest.getResult()){
											smallest = playerList.get(i);
										}	
									}
								}
								
								try{
									message.reply("@" + smallest.getPlayerID() + "  owes " + " gold to " + "@" + largest.getPlayerID() + "  !!");
								}catch(NullPointerException e){
									message.reply("AN ILLUSION!   Resetting.");
									resetGame();
								}
								break;
							default: 
								message.reply("Something's not right... Resetting.");
								
								resetGame();
								break;

							}

						}

					}
				});




				//-----------------------
				// END OF MAIN BOT CODE
				//-----------------------

			}

			public void onFailure(Throwable t){
				t.printStackTrace();
			}
		});

	}
	
	private static int rolled(){
		int roll = 0;
		
		return roll;
	}
	
	private static void resetGame(){
		
	}

}
