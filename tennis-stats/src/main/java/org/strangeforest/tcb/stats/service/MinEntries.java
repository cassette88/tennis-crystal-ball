package org.strangeforest.tcb.stats.service;

import java.time.*;
import java.util.*;

import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.*;

import com.google.common.collect.*;

import static java.lang.Math.*;

@Service
public class MinEntries {

	@Autowired
	private TournamentService tournamentService;

	private static final int MIN_ENTRIES_SEASON_FACTOR =  10;
	private static final int MIN_ENTRIES_EVENT_FACTOR  = 100;
	private static final Map<String, Double> MIN_ENTRIES_LEVEL_WEIGHT_MAP = ImmutableMap.<String, Double>builder()
		.put("G",      0.25)
		.put("F",      0.05)
		.put("L",      0.05)
		.put("M",      0.25)
		.put("O",      0.025)
		.put("A",      0.25)
		.put("B",      0.25)
		.put("D",      0.1)
		.put("T",      0.025)
		.put("GFLMO",  0.5)
		.put("AB",     0.5)
		.put("FL",     0.05)
		.put("DT",     0.1)
		.put("GLD",    0.25)
		.put("FMOABT", 0.75)
	.build();
	private static final Map<String, Double> MIN_ENTRIES_SURFACE_WEIGHT_MAP = ImmutableMap.<String, Double>builder()
		.put("H", 0.5)
		.put("C", 0.5)
		.put("G", 0.25)
		.put("P", 0.25)
	.build();
	private static final Map<String, Double> MIN_ENTRIES_ROUND_WEIGHT_MAP = ImmutableMap.<String, Double>builder()
		.put("F",    0.1)
		.put("BR",   0.005)
		.put("BR+",  0.1)
		.put("SF",   0.1)
		.put("SF+",  0.15)
		.put("QF",   0.15)
		.put("QF+",  0.2)
		.put("R16",  0.25)
		.put("R16+", 0.5)
		.put("R32",  0.5)
		.put("R32+", 0.75)
		.put("R64",  0.25)
		.put("R128", 0.1)
		.put("RR",   0.05)
	.build();
	private static final Map<String, Double> MIN_ENTRIES_OPPONENT_WEIGHT_MAP = ImmutableMap.<String, Double>builder()
		.put(Opponent.NO_1.name(), 0.05)
		.put(Opponent.TOP_5.name(), 0.1)
		.put(Opponent.TOP_10.name(), 0.1)
		.put(Opponent.TOP_20.name(), 0.25)
		.put(Opponent.TOP_50.name(), 0.5)
		.put(Opponent.TOP_100.name(), 1.0)
		.put(Opponent.HIGHER_RANKED.name(), 0.25)
		.put(Opponent.LOWER_RANKED.name(), 0.25)
		.put(Opponent.UNDER_18.name(), 0.05)
		.put(Opponent.UNDER_21.name(), 0.2)
		.put(Opponent.UNDER_25.name(), 0.5)
		.put(Opponent.OVER_25.name(), 0.5)
		.put(Opponent.OVER_30.name(), 0.2)
		.put(Opponent.OVER_35.name(), 0.05)
		.put(Opponent.YOUNGER.name(), 0.25)
		.put(Opponent.OLDER.name(), 0.25)
		.put(Opponent.RIGHT_HANDED.name(), 1.0)
		.put(Opponent.LEFT_HANDED.name(), 0.2)
		.put(Opponent.BACKHAND_2.name(), 0.1)
		.put(Opponent.BACKHAND_1.name(), 0.1)
		.put(Opponent.SEEDED.name(), 0.5)
		.put(Opponent.UNSEEDED.name(), 0.5)
		.put(Opponent.QUALIFIER.name(), 0.1)
		.put(Opponent.WILD_CARD.name(), 0.05)
		.put(Opponent.LUCKY_LOSER.name(), 0.05)
		.put(Opponent.PROTECTED_RANKING.name(), 0.005)
		.put(Opponent.SPECIAL_EXEMPT.name(), 0.005)
	.build();
	private static final Map<Range<Integer>, Double> MIN_ENTRIES_TOURNAMENT_FACTOR_MAP = ImmutableMap.<Range<Integer>, Double>builder()
		.put(Range.closed(1, 2), 100.0)
		.put(Range.closed(3, 5), 50.0)
		.put(Range.closed(6, 9), 25.0)
		.put(Range.atLeast(10), 20.0)
	.build();
	private static final int MIN_ENTRIES_COUNTRY_FACTOR = 10;

	public int getFilteredMinEntries(int minEntries, PerfStatsFilter filter) {
		if (filter.hasSeason()) {
			minEntries /= MIN_ENTRIES_SEASON_FACTOR;
			LocalDate today = LocalDate.now();
			if (filter.getSeason() == today.getYear() && today.getMonth().compareTo(Month.SEPTEMBER) <= 0)
				minEntries /= 12.0 / today.getMonth().getValue();
		}
		if (filter.isLast52Weeks())
			minEntries /= MIN_ENTRIES_SEASON_FACTOR;
		if (filter.hasLevel())
			minEntries *= getMinEntriesWeight(filter.getLevel(), MIN_ENTRIES_LEVEL_WEIGHT_MAP);
		if (filter.hasSurface())
			minEntries *= getMinEntriesSummedWeight(filter.getSurface(), MIN_ENTRIES_SURFACE_WEIGHT_MAP);
		if (filter.hasRound())
			minEntries *= getMinEntriesWeight(filter.getRound(), MIN_ENTRIES_ROUND_WEIGHT_MAP);
		if (filter.hasTournamentEvent())
			minEntries /= MIN_ENTRIES_EVENT_FACTOR;
		else if (filter.hasTournament())
			minEntries /= getMinEntriesTournamentFactor(filter.getTournamentId());
		if (filter.hasOpponent()) {
			OpponentFilter opponentFilter = filter.getOpponentFilter();
			if (opponentFilter.hasOpponent())
				minEntries *= getMinEntriesWeight(opponentFilter.getOpponent().name(), MIN_ENTRIES_OPPONENT_WEIGHT_MAP);
			if (opponentFilter.hasCountries())
				minEntries /= MIN_ENTRIES_COUNTRY_FACTOR;
		}
		return max(minEntries, 2);
	}

	private double getMinEntriesWeight(String item, Map<String, Double> weightMap) {
		return weightMap.getOrDefault(item, 1.0);
	}

	private double getMinEntriesSummedWeight(String items, Map<String, Double> weightMap) {
		return min(items.chars().mapToObj(i -> (char)i).mapToDouble(c -> weightMap.getOrDefault(c.toString(), 0.0)).sum(), 1.0);
	}

	private double getMinEntriesTournamentFactor(int tournamentId) {
		int eventCount = tournamentService.getTournamentEventCount(tournamentId);
		return MIN_ENTRIES_TOURNAMENT_FACTOR_MAP.entrySet().stream().filter(entry -> entry.getKey().contains(eventCount)).findFirst().get().getValue();
	}
}
