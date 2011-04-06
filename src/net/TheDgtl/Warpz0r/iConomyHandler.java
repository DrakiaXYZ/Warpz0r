package net.TheDgtl.Warpz0r;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class iConomyHandler {
	public static boolean useiConomy = false;
	public static iConomy iconomy = null;
	public static String inFundMsg = "Insufficient Funds.";
	
	public static double getBalance(String player) {
		if (useiConomy && iconomy != null) {
			Account acc = iConomy.getBank().getAccount(player);
			if (acc == null) {
				Warpz0r.log.info("[Warpz0r::ich::getBalance] Error fetching iConomy account for " + player);
				return 0;
			}
			return acc.getBalance();
		}
		return 0;
	}
	
	public static boolean chargePlayer(String player, double amount) {
		if (amount == 0) return true;
		if (useiConomy && iconomy != null) {
			Account acc = iConomy.getBank().getAccount(player);
			if (acc == null) {
				Warpz0r.log.info("[Warpz0r::ich::chargePlayer] Error fetching iConomy account for " + player);
				return false;
			}
			double balance = acc.getBalance();
			
			if (balance < amount) return false;
			acc.setBalance(balance - amount);
			return true;
		}
		return true;
	}
	
	public static boolean useiConomy() {
		return (useiConomy && iconomy != null);
	}
}
