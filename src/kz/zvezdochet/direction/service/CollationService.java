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
				sql = "insert into " + tableName + " values(0,?,?,?,?,?)";
			else
				sql = "update " + tableName + " set " +
					"calculated = ?, " +
					"eventid = ?, " +
					"description = ?, " +
					"created_at = ?, " +
					"text = ? " +
					"where id = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setInt(1, collation.isCalculated() ? 1 : 0);
			ps.setLong(2, collation.getEventid());
			ps.setString(3, collation.getDescription());
			ps.setString(4, DateUtil.formatCustomDateTime(new Date(), "yyyy-MM-dd HH:mm:ss"));
			ps.setString(5, collation.getText());
			if (model.getId() != null) 
				ps.setLong(6, model.getId());

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

	/**
	 * Поиск участников события
	 * @param collationid идентификатор прогноза
	 * @return список участников
	 */
	public List<Event> findParticipants(Long collationid) throws DataAccessException {
		if (null == collationid) return null;
		List<Event> list = new ArrayList<Event>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + getParticipantTable() + " where collationid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, collationid);
			rs = ps.executeQuery();
			while (rs.next()) {
				Event event = (Event)new EventService().find(rs.getLong("eventid"));
				if (2 == event.getHuman())
					event.setMembers(findMembers(collationid, event.getId()));
				list.add(event);
			}
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
	public Model create() {
		return new Collation();
	}

	@Override
	public Model init(ResultSet rs, Model base) throws DataAccessException, SQLException {
		Collation model = (base != null) ? (Collation)base : (Collation)create();
		model.setId(Long.parseLong(rs.getString("ID")));
		if (rs.getString("description") != null)
			model.setDescription(rs.getString("description"));
		if (rs.getString("text") != null)
			model.setText(rs.getString("text"));
		model.setCreated_at(DateUtil.getDatabaseDateTime(rs.getString("created_at")));
		String s = rs.getString("calculated");
		model.setCalculated(s.equals("1") ? true : false);
		model.setEvent((Event)new EventService().find(rs.getLong("eventid")));
		model.setParticipants(findParticipants(model.getId()));
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
			String table = getParticipantTable();
			String sql = "delete from " + table + " where collationid = " + model.getId();
			Connection conn = Connector.getInstance().getConnection();
			ps = conn.prepareStatement(sql);
			ps.execute();
			ps.close();

			List<Event> participants = model.getParticipants();
			if (participants != null && participants.size() > 0) {
				conn.setAutoCommit(false);
				//сохраняем участников
				sql = "insert into " + table + " values(?,?)";
				ps = conn.prepareStatement(sql);
				for (Event event : participants) {
					ps.setLong(1, model.getId());            
					ps.setLong(2, event.getId());
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
				table = getMemberTable();
				sql = "insert into " + table + " values(?,?,?)";
				ps = conn.prepareStatement(sql);
				int batchcnt = 0;
				for (Event participant : participants) {
					List<Event> members = participant.getMembers();
					if (members != null && members.size() > 0) {
						for (Event member : members) {
							ps.setLong(1, member.getId());
							ps.setLong(2, participant.getId());
							ps.setLong(3, model.getId());
							ps.addBatch();
							++batchcnt;
						}
					}
				}
				if (batchcnt > 0)
					modified = ps.executeBatch();

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

	/**
	 * Возвращает имя таблицы, хранящей участников события
	 * @return имя ТБД
	 */
	public String getParticipantTable() {
		return "collation_participants";
	}

	/**
	 * Возвращает имя таблицы, хранящей фигурантов события
	 * @return имя ТБД
	 */
	public String getMemberTable() {
		return "collation_members";
	}

	/**
	 * Поиск участников сообщества
	 * @param collationid идентификатор прогноза
	 * @param participantid идентификатор сообщества
	 * @return список участников
	 */
	public List<Event> findMembers(Long collationid, Long participantid) throws DataAccessException {
		if (null == collationid || null == participantid) return null;
		List<Event> list = new ArrayList<Event>();
        PreparedStatement ps = null;
        ResultSet rs = null;
		try {
			String sql = "select * from " + getMemberTable() + 
				" where collationid = ?" +
				" and participantid = ?";
			ps = Connector.getInstance().getConnection().prepareStatement(sql);
			ps.setLong(1, collationid);
			ps.setLong(2, participantid);
			rs = ps.executeQuery();
			while (rs.next())
				list.add((Event)new EventService().find(rs.getLong("memberid")));
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
