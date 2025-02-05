package org.finos.springbot.symphony.stream.fixture;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.finos.springbot.symphony.stream.Participant;
import org.finos.springbot.symphony.stream.cluster.LeaderService;

public class DummyLeaderServiceImpl implements LeaderService {

	public List<Participant> leaderHistory = new ArrayList<Participant>();
	Set<Participant> knownParticipants;
	
	public DummyLeaderServiceImpl(Set<Participant> knownParticipants) {
		super();
		this.knownParticipants = knownParticipants;
	}

	@Override
	public synchronized void becomeLeader(Participant p) {
		leaderHistory.add(p);
		System.out.println("Became leader: "+p+" ("+leaderHistory.size()+")");
	}

	@Override
	public boolean isLeader(Participant p) {
		if (leaderHistory.size() == 0) {
			return false;
		}
		return leaderHistory.get(leaderHistory.size()-1).equals(p);
	}

	@Override
	public List<Participant> getRecentParticipants() {
		return new ArrayList<Participant>(knownParticipants);
	}

}
