/*
Copyright (C) 2013 u.wol@wwu.de 
 
This file is part of ComputationalEconomy.

ComputationalEconomy is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ComputationalEconomy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ComputationalEconomy. If not, see <http://www.gnu.org/licenses/>.
 */

package compecon.economy.sectors.financial;


public interface BankAccount {

	public enum TermType {
		SHORT_TERM, LONG_TERM;
	}

	public enum MoneyType {
		DEPOSITS, CENTRALBANK_MONEY
	}

	public void deposit(final double amount);

	public double getBalance();

	public int getId();

	public TermType getTermType();

	public Currency getCurrency();

	public MoneyType getMoneyType();

	public Bank getManagingBank();

	public String getName();

	public boolean getOverdraftPossible();

	public BankCustomer getOwner();

	public void withdraw(final double amount);

}
