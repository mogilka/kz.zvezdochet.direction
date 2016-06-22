package kz.zvezdochet.direction.handler;

import java.util.List;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.part.CollationPart;
import kz.zvezdochet.service.AspectService;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Расчёт группового прогноза
 * @author Nataly Didenko
 *
 */
public class CalcCollationHandler extends Handler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Групповой расчёт", false);
			CollationPart collationPart = (CollationPart)activePart.getObject();
			Collation collation = (Collation)collationPart.getModel(0, false);
			if (null == collation) {
				DialogUtil.alertWarning("Выберите или создайте групповой прогноз для расчёта");
				return;
			}

			Event event = collation.getEvent();
			if (null == collation.getEvent()) {
				DialogUtil.alertWarning("Выберите событие для расчёта");
				return;
			}
			if (!event.isCalculated()) {
				event.calc(false);
				updateStatus("Расчётная конфигурация события создана", false);
			}

			List<Event> participants = collation.getParticipants();
			if (null == participants || 0 == participants.size()) {
				DialogUtil.alertInfo("Добавьте участников события");
				return;
			}

			aspects = new AspectService().getMajorList();
			for (Event participant : participants) {
				if (participant.getRectification() != 3) {
					if (!participant.isCalculated()) {
						participant.calc(false);
						updateStatus("Расчётная конфигурация " + participant.getName() + " создана", false);
					}
					System.out.println("Participant " + participant.getName());
					makeTransits(event, participant);
				}
				List<Event> members = participant.getMembers();
				if (members != null && members.size() > 0)
					for (Event member : members) {
						if (!member.isCalculated()) {
							member.calc(false);
							updateStatus("Расчётная конфигурация " + member.getName() + " создана", false);
						}
						System.out.println("Member " + member.getName());
						makeTransits(event, member);
					}
			}
			updateStatus("Групповой прогноз сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	private List<Model> aspects;
//	private Map<Integer, Map<String, Integer>> parts;

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private void calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим транзитный угол
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExactTruncAspect(res)) {
					System.out.println("\t" + point1.getName() + " " + a.getType().getSymbol() + " " + point2.getName() + " = " + res);
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}

	/**
	 * Расчёт транзитов
	 */
	private void makeTransits(Event event, Event person) {
		List<Model> planets = person.getConfiguration().getPlanets();
		for (Model model : planets) {
			Planet planet = (Planet)model;
			//дирекции планеты участника к планетам события
			for (Model model2 : event.getConfiguration().getPlanets()) {
				Planet eplanet = (Planet)model2;
				calc(planet, eplanet);
			}
			//дирекции планеты участника к куспидам домов события
			for (Model model2 : event.getConfiguration().getHouses()) {
				House house = (House)model2;
				calc(planet, house);
			}
		}
	}
}
