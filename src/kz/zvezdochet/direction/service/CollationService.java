package kz.zvezdochet.direction.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.core.tool.Connector;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.bean.Member;
import kz.zvezdochet.direction.bean.Participant;
import kz.zvezdochet.service.EventService;

/**
 * Сервис прогнозов
 * @author Nataly Didenko
 */
public class CollationService extends ModelService {

	public CollationService() {
		tableName = "collation";
	}

	@Override
	public Model save(Model model) throws DataAccessException {
		Collation collation = (Collation)model;
		int result = -1;
        PreparedStatement ps = null;
		try {
			String sql;
			if (null == model.getId()) 
				sql = "insert into " + tableName + " values(0,?,?,?,?)";
			else
				sql = "update " + tableName + " set " +
					"calculated = ?, " +
					"eventid = ?, " +
					"description = ?, " +
					"created_at = ?, " +
					"where id = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setInt(1, collation.isCalculated() ? 1 : 0);
			ps.setLong(2, collation.getEvent().getId());
			ps.setString(3, collation.getDescription());
			ps.setString(4, DateUtil.formatCustomDateTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
			if (model.getId() != null) 
				ps.setLong(5, model.getId());

			result = ps.executeUpdate();
			if (1 == result) {
				if (null == model.getId()) { 
					Long autoIncKeyFromApi = -1L;
					ResultSet rsid = ps.getGeneratedKeys();
					if (rsid.next()) {
				        autoIncKeyFromApi = rsid.getLong(1);
				        model.setId(autoIncKeyFromApi);
					    System.out.println(autoIncKeyFromApi + "\t" + ps);
					}
					if (rsid != null)
						rsid.close();
				}
			}
			if (collation.isNeedSaveRel())
				saveParticipants(collation);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)	ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return model;
	}

	@Override
	public Model create() {
		return new Collation();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException, SQLException {
		Collation model = (base != null) ? (Collation)base : (Collation)create();
		model.setId(Long.parseLong(rs.getString("ID")));
		if (rs.getString("description") != null)
			model.setDescription(rs.getString("description"));
		model.setCreated_at(DateUtil.getDatabaseDateTime(rs.getString("created_at")));
		String s = rs.getString("calculated");
		model.setCalculated(s.equals("1") ? true : false);
		model.setEvent((Event)new EventService().find(rs.getLong("eventid")));
		return model;
	}

	/**
	 * Поиск прогноза по наименованию события
	 * @param text поисковое выражение
	 * @return список прогнозов
	 * @throws DataAccessException
	 */
	public List<Model> findByName(String text) throws DataAccessException {
        List<Model> list = new ArrayList<Model>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select c.* from " + tableName + " c" +
				" inner join " + new EventService().getTableName() + " e on c.eventid = e.id" +
				" where human = 0" +
				" and name like ? " +
				" order by created_at";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setString(1, "%" + text + "%");
			rs = ps.executeQuery();
			while (rs.next())
				list.add(init(rs, null));
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

	/**
	 * Поиск прогнозов событий за период
	 * @param date начальная дата
	 * @param date2 конечная дата
	 * @return список прогнозов
	 * @throws DataAccessException
	 */
	public List<Model> findByDateRange(Date date, Date date2) throws DataAccessException {
        List<Model> list = new ArrayList<Model>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select c.* from " + tableName + " c" +
				" inner join " + new EventService().getTableName() + " e on c.eventid = e.id" +  
				" where human = 0" +
				" and initialdate between ? and ?" +
				" order by created_at";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setString(1, DateUtil.dbdtf.format(date));
			ps.setString(2, DateUtil.dbdtf.format(date2));
			System.out.println(ps);
			rs = ps.executeQuery();
			while (rs.next())
				list.add(init(rs, null));
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

	/**
	 * Добавление участников события
	 * @param model прогноз
	 */
	public void saveParticipants(Collation model) {
        PreparedStatement ps = null;
		try {
			String table = new ParticipantService().getTableName();
			String sql = "delete from " + table + " where collationid = " + model.getId();
			Connection conn = Connector.getInstance().getConnection();
			ps = conn.prepareStatement(sql);
			ps.execute();
			ps.close();

			List<Participant> participants = model.getParticipants();
			if (participants != null && participants.size() > 0) {
				conn.setAutoCommit(false);
				//сохраняем участников
				sql = "insert into " + table + "(collationid, eventid, win) values(?,?,?)";
				ps = conn.prepareStatement(sql);
				for (Participant part : participants) {
					ps.setLong(1, model.getId());
					ps.setLong(2, part.getEvent().getId());
					ps.setInt(3, part.isWin() ? 1 : 0);
					ps.addBatch();
				}
				int[] modified = ps.executeBatch();
				ps.close();
				for (int i = 0; i < modified.length; i++) {
				    if (-2 == modified[i])
				    	System.out.println("Execution " + i + " failed: unknown number of rows modified");
				    else
				    	System.out.println("Execution " + i + " success: " + modified[i] + " rows modified");
				}

				//сохраняем фигурантов
				table = new MemberService().getTableName();
				sql = "insert into " + table + "(eventid, participantid, hit, pass, miss, save, foul, substitute) values(?,?,?,?,?,?,?,?)";
				ps = conn.prepareStatement(sql);
				int batchcnt = 0;
				for (Participant part : participants) {
					List<Member> members = part.getMembers();
					if (members != null && members.size() > 0) {
						for (Member member : members) {
							ps.setLong(1, member.getEvent().getId());
							ps.setLong(2, part.getId());
							ps.setInt(3, member.isHit() ? 1 : 0);
							ps.setInt(4, member.isPass() ? 1 : 0);
							ps.setInt(5, member.isMiss() ? 1 : 0);
							ps.setInt(6, member.isSave() ? 1 : 0);
							ps.setInt(7, member.isFoul() ? 1 : 0);
							ps.setInt(8, member.isSubstitute() ? 1 : 0);
							ps.addBatch();
							++batchcnt;
						}
					}
				}
				if (batchcnt > 0)
					modified = ps.executeBatch();

				//TODO сохранять аспекты, дома и дирекции участников и фигурантов
				conn.commit();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)	ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
