/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017-2024 Ta4j Organization & respective
 * authors (see AUTHORS)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.ta4j.core.rules;

import org.junit.Before;
import org.junit.Test;
import org.ta4j.core.*;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.num.Num;

import java.time.ZonedDateTime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AverageTrueRangeTrailingStopLossRuleTest {
    private BarSeries series;

    @Before
    public void setUp() {
        series = new BaseBarSeriesBuilder().withName("Test Series").build();

        series.addBar(ZonedDateTime.now(), 10, 12, 8, 11, 1000);
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 11, 13, 9, 12, 1000);
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 12, 14, 10, 13, 1000);
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 13, 15, 11, 14, 1000);
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 14, 16, 12, 15, 1000);
    }

    @Test
    public void testStopLossTriggeredOnLongPosition() {
        TradingRecord tradingRecord = new BaseTradingRecord();
        tradingRecord.enter(0, series.getBar(0).getClosePrice(), series.numOf(1));

        AverageTrueRangeTrailingStopLossRule rule = new AverageTrueRangeTrailingStopLossRule(series, 3, 1.0);

        assertFalse(rule.isSatisfied(1, tradingRecord)); // Price is still above stop loss
        assertFalse(rule.isSatisfied(2, tradingRecord)); // Price is still above stop loss

        // Simulate a price drop to trigger stop loss
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 11, 12, 9, 10, 1000);
        assertTrue(rule.isSatisfied(5, tradingRecord)); // Stop loss should trigger now
    }

    @Test
    public void testStopLossTriggeredOnShortPosition() {
        TradingRecord tradingRecord = new BaseTradingRecord(Trade.TradeType.SELL);
        tradingRecord.enter(0, series.getBar(0).getClosePrice(), series.numOf(-1));

        AverageTrueRangeTrailingStopLossRule rule = new AverageTrueRangeTrailingStopLossRule(series, 3, 1.0);

        assertFalse(rule.isSatisfied(1, tradingRecord)); // Price is still below stop loss
        assertFalse(rule.isSatisfied(2, tradingRecord)); // Price is still below stop loss

        // Simulate a price increase to trigger stop loss
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 15, 16, 14, 15, 1000);
        assertTrue(rule.isSatisfied(5, tradingRecord)); // Stop loss should trigger now
    }

    @Test
    public void testStopLossNotTriggered() {
        TradingRecord tradingRecord = new BaseTradingRecord();
        tradingRecord.enter(0, series.getBar(0).getClosePrice(), series.numOf(1));

        AverageTrueRangeTrailingStopLossRule rule = new AverageTrueRangeTrailingStopLossRule(series, 3, 1.0);

        assertFalse(rule.isSatisfied(1, tradingRecord));
        assertFalse(rule.isSatisfied(2, tradingRecord));
        assertFalse(rule.isSatisfied(3, tradingRecord));
    }

    @Test
    public void testCustomReferencePrice() {
        Indicator<Num> customReferencePrice = new ClosePriceIndicator(series);
        AverageTrueRangeTrailingStopLossRule rule = new AverageTrueRangeTrailingStopLossRule(series,
                customReferencePrice, 3, 1.0);

        TradingRecord tradingRecord = new BaseTradingRecord();
        tradingRecord.enter(0, series.getBar(0).getClosePrice(), series.numOf(1));

        assertFalse(rule.isSatisfied(1, tradingRecord));
        assertFalse(rule.isSatisfied(2, tradingRecord));

        // Simulate a price drop to trigger stop loss
        series.addBar(series.getLastBar().getEndTime().plusDays(1), 11, 12, 9, 10, 1000);
        assertTrue(rule.isSatisfied(5, tradingRecord));
    }

    @Test
    public void testEdgeCaseNoTrade() {
        AverageTrueRangeTrailingStopLossRule rule = new AverageTrueRangeTrailingStopLossRule(series, 3, 1.0);

        TradingRecord tradingRecord = new BaseTradingRecord();

        // No trade, so the rule should never be satisfied
        assertFalse(rule.isSatisfied(0, tradingRecord));
        assertFalse(rule.isSatisfied(1, tradingRecord));
        assertFalse(rule.isSatisfied(2, tradingRecord));
    }
}