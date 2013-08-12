/*
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

package compecon.culture.sectors.household;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import compecon.culture.PricingBehaviour;
import compecon.culture.markets.SettlementMarket.ISettlementEvent;
import compecon.culture.sectors.financial.BankAccount;
import compecon.culture.sectors.financial.BankAccount.BankAccountType;
import compecon.culture.sectors.financial.Currency;
import compecon.culture.sectors.state.law.bookkeeping.BalanceSheet;
import compecon.culture.sectors.state.law.property.Property;
import compecon.culture.sectors.state.law.property.PropertyRegister;
import compecon.culture.sectors.state.law.security.equity.IShareOwner;
import compecon.culture.sectors.state.law.security.equity.Share;
import compecon.engine.Agent;
import compecon.engine.AgentFactory;
import compecon.engine.MarketFactory;
import compecon.engine.jmx.Log;
import compecon.engine.time.ITimeSystemEvent;
import compecon.engine.time.TimeSystem;
import compecon.engine.time.calendar.DayType;
import compecon.engine.time.calendar.MonthType;
import compecon.engine.util.MathUtil;
import compecon.nature.materia.GoodType;
import compecon.nature.materia.Refreshable;
import compecon.nature.math.intertemporal.consumption.IntertemporalConsumptionFunction;
import compecon.nature.math.intertemporal.consumption.IrvingFisherIntertemporalConsumptionFunction.Period;
import compecon.nature.math.utility.IUtilityFunction;

/**
 * Agent type Household offers labour hours and consumes goods.
 */
@Entity
public class Household extends Agent implements IShareOwner {

	// configuration constants ------------------------------

	@Transient
	protected final int NEW_HOUSEHOLD_FROM_X_DAYS = 365 * 18;

	@Transient
	protected final int NEW_HOUSEHOLD_EVERY_X_DAYS = 365 * 5;

	@Transient
	protected final int LIFESPAN_IN_DAYS = 365 * 80;

	@Transient
	protected final int RETIREMENT_AGE_IN_DAYS = 365 * 65;

	@Transient
	protected final int DAYS_WITHOUT_UTILITY_UNTIL_DESTRUCTOR = 60 + this
			.hashCode() % 60;

	@Transient
	protected final double REQUIRED_UTILITY = 1.0;

	// dynamic state ------------------------------

	@Column(name = "ageInDays")
	protected int ageInDays = 0;

	@Column(name = "daysWithoutUtility")
	protected int daysWithoutUtility = 0;

	@Column(name = "continuousDaysWithUtility")
	protected int continuousDaysWithUtility = 0;

	@OneToOne
	@JoinColumn(name = "savingsBankAccount_id")
	@Index(name = "IDX_A_SAVINGSBANKACCOUNT")
	// bank account for savings
	protected BankAccount savingsBankAccount;

	@Transient
	protected LabourPower labourPower = new LabourPower();

	@Transient
	protected IntertemporalConsumptionFunction intertemporalConsumptionFunction;

	@Transient
	protected PricingBehaviour pricingBehaviour;

	@Transient
	protected IUtilityFunction utilityFunction;

	@Transient
	protected double dividendSinceLastPeriod;

	@Override
	public void initialize() {
		super.initialize();

		// daily life at random HourType
		ITimeSystemEvent dailyLifeEvent = new DailyLifeEvent();
		this.timeSystemEvents.add(dailyLifeEvent);
		TimeSystem.getInstance()
				.addEvent(dailyLifeEvent, -1, MonthType.EVERY, DayType.EVERY,
						TimeSystem.getInstance().suggestRandomHourType());

		// balance sheet publication
		ITimeSystemEvent balanceSheetPublicationEvent = new BalanceSheetPublicationEvent();
		this.timeSystemEvents.add(balanceSheetPublicationEvent);
		TimeSystem.getInstance().addEvent(balanceSheetPublicationEvent, -1,
				MonthType.EVERY, DayType.EVERY,
				BALANCE_SHEET_PUBLICATION_HOUR_TYPE);

		double marketPrice = MarketFactory.getInstance().getMarginalPrice(
				this.primaryCurrency, GoodType.LABOURHOUR);
		this.pricingBehaviour = new PricingBehaviour(this, GoodType.LABOURHOUR,
				this.primaryCurrency, marketPrice);
		this.labourPower.refresh();
	}

	@Transient
	public void deconstruct() {
		super.deconstruct();

		this.savingsBankAccount = null;
	}

	/*
	 * accessors
	 */

	public int getAgeInDays() {
		return ageInDays;
	}

	public int getContinuousDaysWithUtility() {
		return continuousDaysWithUtility;
	}

	public int getDaysWithoutUtility() {
		return daysWithoutUtility;
	}

	public IntertemporalConsumptionFunction getIntertemporalConsumptionFunction() {
		return intertemporalConsumptionFunction;
	}

	public PricingBehaviour getPricingBehaviour() {
		return pricingBehaviour;
	}

	public BankAccount getSavingsBankAccount() {
		return savingsBankAccount;
	}

	public IUtilityFunction getUtilityFunction() {
		return utilityFunction;
	}

	public void setAgeInDays(int ageInDays) {
		this.ageInDays = ageInDays;
	}

	public void setContinuousDaysWithUtility(int continuousDaysWithUtility) {
		this.continuousDaysWithUtility = continuousDaysWithUtility;
	}

	public void setDaysWithoutUtility(int daysWithoutUtility) {
		this.daysWithoutUtility = daysWithoutUtility;
	}

	public void setIntertemporalConsumptionFunction(
			IntertemporalConsumptionFunction intertemporalConsumptionFunction) {
		this.intertemporalConsumptionFunction = intertemporalConsumptionFunction;
	}

	public void setPricingBehaviour(PricingBehaviour pricingBehaviour) {
		this.pricingBehaviour = pricingBehaviour;
	}

	public void setSavingsBankAccount(BankAccount savingsBankAccount) {
		this.savingsBankAccount = savingsBankAccount;
	}

	public void setUtilityFunction(IUtilityFunction utilityFunction) {
		this.utilityFunction = utilityFunction;
	}

	/*
	 * assertions
	 */

	@Transient
	public void assureDividendBankAccount() {
		this.assureTransactionsBankAccount();
	}

	@Transient
	public void assureSavingsBankAccount() {
		if (this.isDeconstructed)
			return;

		this.assureBankCustomerAccount();

		// initialize bank account
		if (this.savingsBankAccount == null) {
			this.savingsBankAccount = this.primaryBank.openBankAccount(this,
					this.primaryCurrency,
					this.bankPasswords.get(this.primaryBank),
					"savings account", BankAccountType.SAVINGS);
		}
	}

	/*
	 * business logic
	 */

	@Override
	@Transient
	public BankAccount getDividendBankAccount() {
		this.assureTransactionsBankAccount();

		return this.transactionsBankAccount;
	}

	@Override
	@Transient
	public void onDividendTransfer(double dividendAmount) {
		this.dividendSinceLastPeriod += dividendAmount;
	}

	@Override
	@Transient
	public void onBankCloseBankAccount(BankAccount bankAccount) {
		if (this.savingsBankAccount != null
				&& this.savingsBankAccount == bankAccount) {
			this.savingsBankAccount = null;
		}

		super.onBankCloseBankAccount(bankAccount);
	}

	public class SettlementMarketEvent implements ISettlementEvent {
		@Override
		public void onEvent(GoodType goodType, double amount,
				double pricePerUnit, Currency currency) {
			Household.this.assureTransactionsBankAccount();
			if (goodType.equals(GoodType.LABOURHOUR)) {
				Household.this.pricingBehaviour.registerSelling(amount, amount
						* pricePerUnit);

				/*
				 * no exhaust of labour hours, as the offered labour hours have
				 * been transfered by the SettelementMarket via PropertyRegister
				 * and will be exhausted by the buyer
				 */
			}
		}

		@Override
		public void onEvent(Currency commodityCurrency, double amount,
				double pricePerUnit, Currency currency) {
		}

		@Override
		public void onEvent(Property property, double pricePerUnit,
				Currency currency) {
		}

	}

	public class BalanceSheetPublicationEvent implements ITimeSystemEvent {
		@Override
		public void onEvent() {
			Household.this.assureTransactionsBankAccount();
			Household.this.assureSavingsBankAccount();

			BalanceSheet balanceSheet = Household.this.issueBasicBalanceSheet();

			// bank deposits
			if (Household.this.savingsBankAccount.getBalance() > 0)
				balanceSheet.cashLongTerm += Household.this.savingsBankAccount
						.getBalance();
			else
				balanceSheet.loans += -1
						* Household.this.savingsBankAccount.getBalance();

			Log.agent_onPublishBalanceSheet(Household.this, balanceSheet);
		}
	}

	public class DailyLifeEvent implements ITimeSystemEvent {

		@Override
		public void onEvent() {
			if (Household.this.isDeconstructed)
				throw new RuntimeException(Household.this
						+ " is deconstructed, but not removed from TimeSystem");

			/*
			 * potentially call destructor
			 */
			if (Household.this.ageInDays > LIFESPAN_IN_DAYS) {
				Household.this.deconstruct();
				return;
			}

			/*
			 * simulation mechanics
			 */
			Household.this.ageInDays++;
			Household.this.labourPower.refresh();
			Household.this.pricingBehaviour.nextPeriod();

			/*
			 * actions
			 */
			Household.this.assureTransactionsBankAccount();
			Household.this.assureSavingsBankAccount();

			double budget = this.planConsumptionAndSaving();

			Map<GoodType, Double> plannedConsumptionGoodsBundle = this
					.buyGoods(budget);

			double utility = this.consumeGoods(plannedConsumptionGoodsBundle);

			this.offerLabourHours();

			this.buyAndOfferShares();

			/*
			 * check for required utility
			 */
			if (utility < Household.this.REQUIRED_UTILITY) {
				Household.this.daysWithoutUtility++;
				Household.this.continuousDaysWithUtility = 0;
				if (Log.isAgentSelectedByClient(Household.this))
					Log.log(Household.this, DailyLifeEvent.class,
							"does not have required utility of "
									+ Household.this.REQUIRED_UTILITY);
			} else {
				if (Household.this.daysWithoutUtility > 0)
					daysWithoutUtility--;
				Household.this.continuousDaysWithUtility++;
			}

			/*
			 * potentially, derive new household
			 */
			if (Household.this.ageInDays >= NEW_HOUSEHOLD_FROM_X_DAYS) {
				if ((Household.this.ageInDays - NEW_HOUSEHOLD_FROM_X_DAYS)
						% NEW_HOUSEHOLD_EVERY_X_DAYS == 0) {
					AgentFactory
							.newInstanceHousehold(Household.this.primaryCurrency);
				}
			}

			/*
			 * potentially, call destructor
			 */
			if (Household.this.daysWithoutUtility > Household.this.DAYS_WITHOUT_UTILITY_UNTIL_DESTRUCTOR)
				if (!TimeSystem.getInstance().isInitializationPhase())
					Household.this.deconstruct();
		}

		public double planConsumptionAndSaving() {
			/*
			 * calculate budget
			 */
			double keyInterestRate = AgentFactory.getInstanceCentralBank(
					Household.this.primaryCurrency)
					.getEffectiveKeyInterestRate();
			double income = Household.this.transactionsBankAccount.getBalance();
			Map<Period, Double> intertemporalConsumptionPlan = Household.this.intertemporalConsumptionFunction
					.calculateUtilityMaximizingConsumptionPlan(income,
							Household.this.savingsBankAccount.getBalance(),
							keyInterestRate, Household.this.ageInDays,
							Household.this.RETIREMENT_AGE_IN_DAYS,
							Household.this.LIFESPAN_IN_DAYS
									- Household.this.ageInDays);
			double budget = intertemporalConsumptionPlan.get(Period.CURRENT);
			double moneySumToConsume = budget;
			double moneySumToSave = income - moneySumToConsume;

			/*
			 * logging
			 */
			Log.household_onIncomeWageDividendConsumptionSaving(
					primaryCurrency, income, moneySumToConsume, moneySumToSave,
					Household.this.pricingBehaviour.getLastSoldValue(),
					Household.this.dividendSinceLastPeriod);

			Household.this.dividendSinceLastPeriod = 0;

			// if not retired
			if (Household.this.ageInDays < Household.this.RETIREMENT_AGE_IN_DAYS) {
				/*
				 * save money for retirement
				 */
				if (Log.isAgentSelectedByClient(Household.this))
					Log.log(Household.this,
							DailyLifeEvent.class,
							"saving "
									+ Currency.round(moneySumToSave)
									+ " "
									+ Household.this.transactionsBankAccount
											.getCurrency().getIso4217Code()
									+ " of "
									+ Currency
											.round(Household.this.transactionsBankAccount
													.getBalance())
									+ " "
									+ Household.this.transactionsBankAccount
											.getCurrency().getIso4217Code()
									+ " income");
				if (MathUtil.greater(moneySumToSave, 0.0)) {
					Household.this.transactionsBankAccount
							.getManagingBank()
							.transferMoney(
									Household.this.transactionsBankAccount,
									Household.this.savingsBankAccount,
									moneySumToSave,
									Household.this.bankPasswords
											.get(Household.this.transactionsBankAccount
													.getManagingBank()),
									"retirement savings");
				}
			}
			// retired
			else {
				/*
				 * spend saved retirement money
				 */
				if (Log.isAgentSelectedByClient(Household.this))
					Log.log(Household.this,
							"unsaving "
									+ Currency.round(budget)
									+ " "
									+ Household.this.transactionsBankAccount
											.getCurrency().getIso4217Code());
				Household.this.savingsBankAccount.getManagingBank()
						.transferMoney(
								Household.this.savingsBankAccount,
								Household.this.transactionsBankAccount,
								budget,
								Household.this.bankPasswords
										.get(Household.this.savingsBankAccount
												.getManagingBank()),
								"retirement dissavings");
			}

			return budget;
		}

		public Map<GoodType, Double> buyGoods(double budget) {
			// TODO: what if there are not enough goods to buy? -> higher saving

			/*
			 * buy goods -> maximize utility
			 */
			Map<GoodType, Double> prices = MarketFactory.getInstance()
					.getMarginalPrices(
							Household.this.transactionsBankAccount
									.getCurrency());
			Map<GoodType, Double> plannedConsumptionGoodsBundle = Household.this.utilityFunction
					.calculateUtilityMaximizingInputsUnderBudgetRestriction(
							prices, budget);

			for (Entry<GoodType, Double> entry : plannedConsumptionGoodsBundle
					.entrySet()) {
				GoodType goodType = entry.getKey();
				if (!GoodType.LABOURHOUR.equals(goodType)) {
					double maxAmount = entry.getValue();
					double maxTotalPrice = Household.this.transactionsBankAccount
							.getBalance();
					if (!Double.isInfinite(maxAmount)) {
						maxTotalPrice = Math.min(
								maxAmount * prices.get(goodType),
								Household.this.transactionsBankAccount
										.getBalance());
					}
					double[] priceAndAmount = MarketFactory.getInstance().buy(
							goodType,
							maxAmount,
							maxTotalPrice,
							prices.get(goodType),
							Household.this,
							Household.this.transactionsBankAccount,
							Household.this.bankPasswords
									.get(Household.this.transactionsBankAccount
											.getManagingBank()));
				}
			}
			return plannedConsumptionGoodsBundle;
		}

		public double consumeGoods(
				Map<GoodType, Double> plannedConsumptionGoodsBundle) {
			/*
			 * consume goods
			 */
			Map<GoodType, Double> effectiveConsumptionGoodsBundle = new HashMap<GoodType, Double>();
			for (GoodType goodType : Household.this.utilityFunction
					.getInputGoodTypes()) {
				double balance = PropertyRegister.getInstance().getBalance(
						Household.this, goodType);
				double amountToConsume = Math.min(balance,
						plannedConsumptionGoodsBundle.get(goodType));
				effectiveConsumptionGoodsBundle.put(goodType, amountToConsume);
				PropertyRegister.getInstance().decrementGoodTypeAmount(
						Household.this, goodType, amountToConsume);
			}
			double utility = Household.this.utilityFunction
					.calculateUtility(effectiveConsumptionGoodsBundle);
			Log.household_onUtility(Household.this,
					Household.this.transactionsBankAccount.getCurrency(),
					effectiveConsumptionGoodsBundle, utility);
			return utility;
		}

		public void offerLabourHours() {
			/*
			 * remove labour hour offers
			 */
			MarketFactory.getInstance().removeAllSellingOffers(Household.this,
					Household.this.transactionsBankAccount.getCurrency(),
					GoodType.LABOURHOUR);

			// if not retired
			if (Household.this.ageInDays < Household.this.RETIREMENT_AGE_IN_DAYS) {
				/*
				 * offer labour hours
				 */
				Household.this.pricingBehaviour.setNewPrice();
				double amountOfLabourHours = PropertyRegister.getInstance()
						.getBalance(Household.this, GoodType.LABOURHOUR);
				MarketFactory.getInstance().placeSettlementSellingOffer(
						GoodType.LABOURHOUR, Household.this,
						Household.this.transactionsBankAccount,
						amountOfLabourHours,
						Household.this.pricingBehaviour.getCurrentPrice(),
						new SettlementMarketEvent());
				Household.this.pricingBehaviour
						.registerOfferedAmount(amountOfLabourHours);
			}
		}

		public void buyAndOfferShares() {
			/*
			 * buy shares / capital -> equity savings
			 */
			MarketFactory.getInstance().buy(
					Share.class,
					1,
					0,
					0,
					Household.this,
					Household.this.transactionsBankAccount,
					Household.this.bankPasswords
							.get(Household.this.primaryBank));

			/*
			 * sell shares that are denominated in an incorrect currency
			 */
			MarketFactory.getInstance().removeAllSellingOffers(Household.this,
					Household.this.transactionsBankAccount.getCurrency(),
					Share.class);
			for (Property property : PropertyRegister.getInstance()
					.getProperties(Household.this, Share.class)) {
				if (property instanceof Share) {
					Share share = (Share) property;
					if (!Household.this.primaryCurrency.equals(share
							.getJointStockCompany().getPrimaryCurrency()))
						MarketFactory.getInstance().placeSellingOffer(property,
								Household.this,
								Household.this.getTransactionsBankAccount(),
								0.0);
				}
			}
		}
	}

	protected class LabourPower implements Refreshable {
		protected final int NUMBER_OF_DAILY_LABOUR_HOURS = 16;

		public double getNumberOfLabourHoursAvailable() {
			return PropertyRegister.getInstance().getBalance(Household.this,
					GoodType.LABOURHOUR);
		}

		@Override
		public boolean isExhausted() {
			return PropertyRegister.getInstance().getBalance(Household.this,
					GoodType.LABOURHOUR) <= 0;
		}

		@Override
		public void exhaust() {
			PropertyRegister.getInstance().resetGoodTypeAmount(Household.this,
					GoodType.LABOURHOUR);
		}

		@Override
		public void refresh() {
			exhaust();
			PropertyRegister.getInstance().incrementGoodTypeAmount(
					Household.this, GoodType.LABOURHOUR,
					this.NUMBER_OF_DAILY_LABOUR_HOURS);
			Log.household_LabourHourCapacity(Household.this,
					this.NUMBER_OF_DAILY_LABOUR_HOURS);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " [" + this.ageInDays / 365 + " years]";
	}
}
