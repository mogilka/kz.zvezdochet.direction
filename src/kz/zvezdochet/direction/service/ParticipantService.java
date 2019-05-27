package kz.zvezdochet.direction.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.bean.Participant;
import kz.zvezdochet.service.EventService;

/**
 * Сервис участника прогноза
 * @author Natalie Didenko
 */
public class ParticipantService extends ModelService {

	public ParticipantService() {
		tableName = "participant";
	}

	@Override
	public Model create() {
		return new Participant();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException, SQLException {
		Participant model = (base != null) ? (Participant)base : (Participant)create();
		model.setId(Long.parseLong(rs.getString("ID")));
		String s = rs.getString("win");
		model.setWin(s.equals("1") ? true : false);
		model.setEvent((Event)new EventService().find(rs.getLong("eventid")));
		model.setCollation((Collation)new CollationService().find(rs.getLong("collationid")));
		model.setMembers(new MemberService().finds(model.getId()));
		return model;
	}

	/**
	 * Поиск участников события
	 * @param collationid идентификатор прогноза
	 * @return список участников
	 */
	public List<Participant> finds(Long collationid) throws DataAccessException {
		if (null == collationid) return null;
		List<Participant> list = new ArrayList<Participant>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName + " where collationid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, collationid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add((Participant)init(rs, null));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try { 
				if (rs != null) rs.close();
				if (ps != null) ps.close();
			} catch (SQLException e) { 
				e.printStackTrace(); 
			}
		}
		return list;
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		return null;
	}
}
