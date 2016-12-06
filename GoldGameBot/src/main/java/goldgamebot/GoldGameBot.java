package goldgamebot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.google.common.util.concurrent.FutureCallback;

import de.btobastian.javacord.DiscordAPI;
import de.btobastian.javacord.Javacord;
import de.btobastian.javacord.entities.message.Message;
import de.btobastian.javacord.listener.message.MessageCreateListener;

public class GoldGameBot {

	private static Properties prop;

	private static boolean isGameRunning = false;
	private static boolean isBetSet = false;
	private static boolean isRegistrationClosed = false;
	private static int betAmount = 0;
	private static List<Player> playerList;
	private static int numPlayersRolled = 0;
	private static String gameOwner;
	private static String[] whoCanReset = null;


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
		final String allowedToReset = prop.getProperty("canReset");
		
		
		if(allowedToReset.contains(",")){
			whoCanReset = allowedToReset.split(",");
		}else{
			whoCanReset[0] = allowedToReset;
		}

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
						boolean fromBoundChannel = false;
						String playerId = message.getAuthor().getId();

						if(!boundChannel.isEmpty())
							isBound = true;

						if(message.getReceiver().getId().equalsIgnoreCase(boundChannel) || !isBound)
							fromBoundChannel = true;


						// START GOLD GAME
						if(fromBoundChannel && message.getContent().toUpperCase().startsWith(commandPrefix + "GOLDGAME")){

							String action = message.getContent().toUpperCase().substring(10);
							System.out.println("Action = " + action);

							int command = 0;

							// DETERMINE WHICH COMMAND WAS USED
							if(action.toUpperCase().startsWith("START")){
								command = 1;
							}else if(action.toUpperCase().startsWith("BET")){
								command = 2;
							}else if(action.toUpperCase().startsWith("JOIN")){
								command = 3;
							}else if(action.toUpperCase().startsWith("CLOSE")){
								command = 4;
							}else if(action.toUpperCase().startsWith("ROLL")){
								command = 5;
							}else if(action.toUpperCase().startsWith("RESET")){
								command = 6;
							}else if(action.toUpperCase().startsWith("HELP")){
								command = 7;
							}

							// EXECUTE THE COMMAND
							switch(command){

							case 1: // START
								if(!isGameRunning){
									isGameRunning = true;
									gameOwner = message.getAuthor().getId();
									message.reply("Starting a new Gold Game.  Please specify a bet amount using '"
											+ commandPrefix + "GOLDGAME BET <AMOUNT>'" );
								}else{
									message.reply("@" + playerId + " A Gold Game is already running!");
								}
								break;
							case 2: // BET
								if(isGameRunning && message.getAuthor().getId().equalsIgnoreCase(gameOwner)){
									try{
										betAmount = Integer.parseInt(message.getContent().substring(14));
									}catch(Exception e){
										message.reply("Amount not recognized.  Please re-enter the bet amount.");
										break;
									}
									isBetSet = true;
									message.reply("Bet amount for the current game is now set to " + betAmount + " gold.\n"
											+ "To join the current Gold Game, please use the command '" + commandPrefix + 
											"GOLDGAME JOIN'" );
								}else if(isGameRunning){
									message.reply("@" + playerId + " You do not have permission to set the bet amount for this round.");
								}else{
									message.reply("@" + playerId + " No Gold Game is currently in-progress.");
								}
								break;
							case 3: // JOIN
								if(isBetSet && isGameRunning && !isRegistrationClosed){
									Player player = new Player(playerId);
									playerList.add(player);
									message.reply("@" + playerId + " has joined this round!  Players joined: " + playerList.size());
								}else{
									if(isGameRunning && isBetSet && isRegistrationClosed)
										message.reply("@" + playerId + " Registration for this round has already closed.");
									else if(!isBetSet && isGameRunning)
										message.reply("Bet amount has not been set yet.  Please wait.");
									else if(!isGameRunning)
										message.reply("@" + playerId + " There is no Gold Game currently running.");
									else
										message.reply("Something went wrong... (ERROR 01)");
								}
								break;
							case 4: // CLOSE
								boolean isOwner = message.getAuthor().getId().equalsIgnoreCase(gameOwner);
								if(isBetSet && isGameRunning && (playerList.size() >= 2) && isOwner){
									isRegistrationClosed = true;
									message.reply("Registration for this round is now closed!  Roll using '" + commandPrefix + "GOLDGAME ROLL'");
								}else{
									if(!message.getAuthor().getId().equalsIgnoreCase(gameOwner))
										message.reply("@" + playerId + " You do not have permissions to close registration for the current round.");
									else if(isBetSet && isGameRunning && (playerList.size() < 2))
										message.reply("@" + playerId + " Not enough players have joined this round yet!");
									else if(isGameRunning)
										message.reply("@" + playerId + " The bet amount has not been set yet!");
									else if(!isGameRunning)
										message.reply("@" + playerId + " There is no Gold Game currently running.");
									else
										message.reply("@" + playerId + " Something went wrong... (ERROR 02).");
								}
								break;
							case 5: // ROLL

								if(isGameRunning && isBetSet && isRegistrationClosed){

									Random generator = new Random();
									int result = generator.nextInt(betAmount);

									for(int n = 0; n < playerList.size(); n++){
										if(playerList.get(n).getPlayerID().equalsIgnoreCase(playerId)){
											playerList.get(n).setResult(result);
											numPlayersRolled++;
										}
									}

									message.reply("@" + playerId + " has rolled " + result);

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
										message.reply("Seomething went wrong. (ERROR 03)");
									}
									resetGame();

								}else{
									if(isGameRunning && isBetSet)
										message.reply("@" + playerId + " Registration has not closed for the current round yet.");
									else if(isGameRunning && !isBetSet)
										message.reply("@" + playerId + " Registration is not open yet. The bet amount still needs to be set.");
									else if(!isGameRunning)
										message.reply("@" + playerId + " There is no Gold Game running!");
									else
										message.reply("Something went wrong. (ERROR 04)");
								}


								break;
							case 6:  // RESET
								String author = message.getAuthor().getId();
								for(int k = 0; k < whoCanReset.length; k++){
									if(author.equalsIgnoreCase(whoCanReset[k]))
										resetGame();
								}
								
								if(message.getAuthor().getId().equalsIgnoreCase(gameOwner))
									resetGame();
								
								break;
							case 7:  // HELP
								message.reply("Welcome to the GoldGameBot HELP.  Here are the available commands: \n" +
										"START - starts a new round of the Gold Game. \n" +
										"BET <Amount> - sets the amount of gold to be bet (game must be running). \n" +
										"JOIN - use this command to join the current round! \n" +
										"CLOSE - ends regestration (once at least 2 players have joined) and begins the ROLL. GLHF! \n" +
										"ROLL - use this command to ROLL! \n\n" +
										"Once all registered players have ROLLed, the bot will display the result. \n\n" +
										"Additional Commands: \n" +
										"RESET - ends the current game. You will then be able to START a new game."
										);
								break;
							default: 
								message.reply("Something's not right... That command was not recognized.");

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

	private static void resetGame(){

		isGameRunning = false;
		isBetSet = false;
		isRegistrationClosed = false;
		betAmount = 0;
		playerList.clear();
		numPlayersRolled = 0;

	}

}
