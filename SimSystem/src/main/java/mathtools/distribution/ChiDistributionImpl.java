/**
 * Copyright 2020 Alexander Herzog
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mathtools.distribution;

import java.io.Serializable;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.special.Gamma;

import mathtools.Functions;

/**
 * Chi-Verteilung (<b>nicht</b> Chi^2-Verteilung)
 * @author Alexander Herzog
 * @version 1.0
 */
public final class ChiDistributionImpl extends AbstractRealDistribution implements Cloneable, Serializable {
	private static final long serialVersionUID = 2215589145625848353L;

	/**
	 * Anzahl an Freiheitsgraden
	 */
	public final int degreesOfFreedom;

	private final double densityDenominator;

	/**
	 * Konstruktor der Klasse
	 * @param degreesOfFreedom	Anzahl an Freiheitsgraden
	 */
	public ChiDistributionImpl(final int degreesOfFreedom) {
		super(null);
		this.degreesOfFreedom=(degreesOfFreedom>0)?degreesOfFreedom:1;
		densityDenominator=Math.pow(2,degreesOfFreedom/2.0-1)*Functions.getGamma(degreesOfFreedom/2.0);
	}

	/**
	 * Copy-Konstruktor
	 * @param source	Zu kopierende Ausgangsverteilung
	 */
	public ChiDistributionImpl(final ChiDistributionImpl source) {
		this((source==null)?1:source.degreesOfFreedom);
	}

	@Override
	public double density(double x) {
		if (x<=0) return 0;
		return Math.exp(-x*x/2)*Math.pow(x,degreesOfFreedom-1)/densityDenominator;
	}

	@Override
	public double cumulativeProbability(double x) {
		if (x<=0) return 0;
		return Gamma.regularizedGammaP(degreesOfFreedom/2.0,x*x/2);
	}

	@Override
	public final ChiDistributionImpl clone() {
		return new ChiDistributionImpl(this);
	}

	@Override
	public double getNumericalMean() {
		return Math.sqrt(2)*Functions.getGamma((degreesOfFreedom+1)/2.0)/Functions.getGamma(degreesOfFreedom/2.0);
	}

	@Override
	public double getNumericalVariance() {
		final double value=Functions.getGamma((degreesOfFreedom+1)/2.0);
		final double numerator=2*Functions.getGamma(degreesOfFreedom/2.0)*Functions.getGamma(1+degreesOfFreedom/2.0)-value*value;
		return numerator/Functions.getGamma(degreesOfFreedom/2.0);
	}

	@Override
	public double getSupportLowerBound() {
		return 0;
	}

	@Override
	public double getSupportUpperBound() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return false;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return false;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}
}
