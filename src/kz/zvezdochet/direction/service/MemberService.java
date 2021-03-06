package kz.zvezdochet.direction.service;

import java.sql.Connection;
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
import kz.zvezdochet.direction.bean.Member;
import kz.zvezdochet.service.EventService;

/**
 * Сервис фигуранта прогноза
 * @author Natalie Didenko
 */
public class MemberService extends ModelService {

	public MemberService() {
		tableName = "member";
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		Member member = (Member)model;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int result = -1;
		try {
			String sql = "select * from " + tableName + " where id = ?";
			Connection conn = Connector.getInstance().getConnection();
			ps = conn.prepareStatement(sql);
			ps.setLong(1, member.getId() != null ? member.getId() : 0);
			rs = ps.executeQuery();
			boolean exists = rs.next();
			if (exists)
				sql = "update " + tableName + " set " +
					"eventid = ?, " +
					"participantid = ?, " +
					"hit = ?, " +
					"pass = ?, " +
					"miss = ?, " +
					"save = ?, " +
					"foul = ?, " +
					"substitute = ?, " +
					"injury = ? " +
					"where id = ?";
			else
				sql = "insert into " + tableName + " values(0,?,?,?,?,?,?,?,?,?)";
			ps.close();

			ps = conn.prepareStatement(sql);
			ps.setLong(1, member.getEvent().getId());
			ps.setLong(2, member.getParticipant().getId());
			ps.setInt(3, member.isHit() ? 1 : 0);
			ps.setInt(4, member.isPass() ? 1 : 0);
			ps.setInt(5, member.isMiss() ? 1 : 0);
			ps.setInt(6, member.isSave() ? 1 : 0);
			ps.setInt(7, member.isFoul() ? 1 : 0);
			ps.setInt(8, member.isSubstitute() ? 1 : 0);
			ps.setInt(9, member.isInjury() ? 1 : 0);
			if (exists) 
				ps.setLong(10, member.getId());
			result = ps.executeUpdate();
			if (1 == result) {
				if (exists) { 
					Long autoIncKeyFromApi = -1L;
					ResultSet rsid = ps.getGeneratedKeys();
					if (rsid.next()) {
						autoIncKeyFromApi = rsid.getLong(1);
						member.setId(autoIncKeyFromApi);
						System.out.println(autoIncKeyFromApi + "\t" + ps);
					}
					if (rsid != null)
						rsid.close();
				}
			}
			ps.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)	ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return member;
	}

	@Override
	public Model create() {
		return new Member();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException, SQLException {
		Member model = (base != null) ? (Member)base : (Member)create();
		model.setId(Long.parseLong(rs.getString("ID")));
		model.setEvent((Event)new EventService().find(rs.getLong("eventid")));
		model.setParticipantid(rs.getLong("participantid"));
		String s = rs.getString("hit");
		model.setHit(s.equals("1") ? true : false);
		s = rs.getString("pass");
		model.setPass(s.equals("1") ? true : false);
		s = rs.getString("miss");
		model.setMiss(s.equals("1") ? true : false);
		s = rs.getString("save");
		model.setSave(s.equals("1") ? true : false);
		s = rs.getString("foul");
		model.setFoul(s.equals("1") ? true : false);
		s = rs.getString("substitute");
		model.setSubstitute(s.equals("1") ? true : false);
		s = rs.getString("injury");
		model.setInjury(s.equals("1") ? true : false);
		return model;
	}

	/**
	 * Поиск фигуранта
	 * @param participantid идентификатор сообщества
	 * @return список фигурантов
	 */
	public List<Member> finds(Long participantid) throws DataAccessException {
		if (null == participantid) return null;
		List<Member> list = new ArrayList<Member>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + tableName + " where participantid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, participantid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add((Member)init(rs, null));
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
}
