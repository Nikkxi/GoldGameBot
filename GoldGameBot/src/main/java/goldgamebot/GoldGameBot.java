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
	static int betAmount;
	static List<String> players;


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
							
							// TODO Need to adjust this to test for if the command has a follow-up word or not
							String[] parsed = action.split(" ", 2);
							String word = parsed[0];

							int command = 0;

							if(word.equalsIgnoreCase("start"))
								command = 1;
							if(word.equalsIgnoreCase("bet"))
								command = 2;
							if(word.equalsIgnoreCase("add"))
								command = 3;
							if(word.equalsIgnoreCase("close"))
								command = 4;
							if(word.equalsIgnoreCase("roll"))
								command = 5;



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
									players = new ArrayList<String>();
								}else{
									message.reply("@" + message.getAuthor().getId() + " No Gold Game is currently in-progress.");
								}
								break;
							case 3: // ADD
								if(isBetSet && isGameRunning && !isGameClosed){
									String player = message.getAuthor().getName();
									players.add(player);
									message.reply("@" + message.getAuthor().getId() + " has joined this round!");
								}else{
									if(!isBetSet)
										message.reply("Bet amount has not been set yet.  Please wait.");
									if(isGameClosed)
										message.reply("@" + message.getAuthor().getId() + " Registration for this round has already closed.");
								}
								break;
							case 4: // CLOSE
								if(isBetSet && isGameRunning && (players.size() >= 2) ){
									isGameClosed = true;
									message.reply("Registration for this round is now closed!  Roll using '" + commandPrefix + "GOLDGAME ROLL'");
								}else{
									message.reply("@" + message.getAuthor().getId() + " Not enough players have joined this round!");
								}
								break;
							case 5: // ROLL
								List<String> hasRolled = new ArrayList<String>();
								
								// TODO need to add code to handle the rolling
								
								hasRolled.add(message.getAuthor().getName());
								break;
							default: 
								message.reply("Something's not right...");
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

}
