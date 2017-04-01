package org.strangeforest.tcb.dataload

loadTournaments(new SqlPool())

static loadTournaments(SqlPool sqlPool) {
	sqlPool.withSql {sql ->
		def atpInProgressTournamentLoader = new ATPWorldTourInProgressTournamentLoader(sql)
		atpInProgressTournamentLoader.loadTournament('miami', 403)
	}
}