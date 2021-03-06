/*
Copyright (C) 2015 u.wol@wwu.de

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

package compecon.engine.factory.impl;

import compecon.economy.agent.impl.AgentImpl;
import compecon.economy.behaviour.PricingBehaviour;
import compecon.economy.behaviour.impl.PricingBehaviourImpl;
import compecon.economy.sectors.financial.Currency;
import compecon.engine.factory.PricingBehaviourFactory;

public class PricingBehaviourFactoryImpl implements PricingBehaviourFactory {

	@Override
	public PricingBehaviour newInstancePricingBehaviour(final AgentImpl agent,
			final Object offeredObject, final Currency denominatedInCurrency,
			final double initialPrice) {
		return new PricingBehaviourImpl(agent, offeredObject,
				denominatedInCurrency, initialPrice);
	}

	@Override
	public PricingBehaviour newInstancePricingBehaviour(final AgentImpl agent,
			final Object offeredObject, final Currency denominatedInCurrency,
			final double initialPrice, final double priceChangeIncrement) {
		return new PricingBehaviourImpl(agent, offeredObject,
				denominatedInCurrency, initialPrice, priceChangeIncrement);
	}

}
