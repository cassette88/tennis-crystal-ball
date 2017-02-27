package org.strangeforest.tcb.dataload

loadTournaments(new SqlPool())

static loadTournaments(SqlPool sqlPool) {
//	def matchLoader = new MatchLoader(sqlPool)
//	matchLoader.load(fetchTournament(2016, 'Quito', 7161))
	sqlPool.withSql {sql ->
		def atpWorldTourMatchLoader = new ATPWorldTourTournamentLoader(sql)
		atpWorldTourMatchLoader.loadTournament(2017, 'montpellier', 375)
		atpWorldTourMatchLoader.loadTournament(2017, 'quito', 7161)
		atpWorldTourMatchLoader.loadTournament(2017, 'sofia', 7434)
		atpWorldTourMatchLoader.loadTournament(2017, 'buenos-aires', 506)
		atpWorldTourMatchLoader.loadTournament(2017, 'memphis', 402)
		atpWorldTourMatchLoader.loadTournament(2017, 'rotterdam', 407)
		atpWorldTourMatchLoader.loadTournament(2017, 'delray-beach', 499)
		atpWorldTourMatchLoader.loadTournament(2017, 'marseille', 496)
		atpWorldTourMatchLoader.loadTournament(2017, 'rio-de-janeiro', 6932)
	}
}