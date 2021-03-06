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

package compecon.simulation.minimal.csv.impl;

import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesChangeListener;

import compecon.economy.materia.GoodType;
import compecon.engine.statistics.ModelRegistry.NationalEconomyModel.PricingBehaviourModel;
import compecon.engine.statistics.accumulator.PeriodDataQuotientAccumulator;

public class SoldCsvWriterImpl extends CsvPeriodWriterImpl implements
		SeriesChangeListener {

	protected final PeriodDataQuotientAccumulator accumulator = new PeriodDataQuotientAccumulator();

	protected final PricingBehaviourModel pricingBehaviourModel;

	public SoldCsvWriterImpl(final String csvFileName,
			final PricingBehaviourModel pricingBehaviourModel,
			final GoodType goodType) {
		super(csvFileName);

		this.pricingBehaviourModel = pricingBehaviourModel;

		writeCsvLine("period", "sold");
	}

	@Override
	public void seriesChanged(final SeriesChangeEvent event) {
		if (pricingBehaviourModel.soldModel != null) {
			final double output = pricingBehaviourModel.soldModel.getValue();

			accumulator.add(output, 1);
		}

		if (isPeriodEnd()) {
			writeCsvLine(getPeriodLabel(),
					Double.toString(accumulator.getAmount()));
			accumulator.reset();
		}
	}
}
