package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.analytics.bean.PlanetHouseText;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.direction.bean.Member;
import kz.zvezdochet.direction.bean.Participant;
import kz.zvezdochet.direction.part.CollationPart;
import kz.zvezdochet.service.AspectService;

/**
 * Расчёт группового прогноза
 * @author Nataly Didenko
 *
 */
public class CalcCollationHandler extends Handler {

	@SuppressWarnings("unchecked")
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
				event.calc(true);
				updateStatus("Расчётная конфигурация события создана", false);
			}

			List<Participant> participants = collation.getParticipants();
			if (null == participants || 0 == participants.size()) {
				DialogUtil.alertInfo("Добавьте участников события");
				return;
			}

			aspects = new AspectService().getMajorList();
			for (Participant participant : participants) {
				Map<String, Object> map = new HashMap<String, Object>();

				Event pevent = participant.getEvent();
				if (!pevent.isCalculated()) {
					pevent.calc(false);
					updateStatus("Расчётная конфигурация " + pevent.getName() + " создана", false);
				}
				map = makeTransits(event, pevent);
				participant.setAspects((List<SkyPointAspect>)map.get("Аспекты"));
				participant.setDirections((List<SkyPointAspect>)map.get("Дирекции"));
				participant.setHouses((List<PlanetHouseText>)map.get("Дома"));

				List<Member> members = participant.getMembers();
				if (members != null && members.size() > 0) {
					Map<String, Object> map2 = new HashMap<String, Object>();
					for (Member member : members) {
						if (null == member.getParticipant())
							member.init(false);
						Event mevent = member.getEvent();
						if (!mevent.isCalculated()) {
							mevent.calc(false);
							updateStatus("Расчётная конфигурация " + mevent.getName() + " создана", false);
						}

						//суммируем данные всех фигурантов участника
						map2 = makeTransits(event, mevent);
						member.setAspects((List<SkyPointAspect>)map2.get("Аспекты"));
						member.setDirections((List<SkyPointAspect>)map2.get("Дирекции"));
						member.setHouses((List<PlanetHouseText>)map2.get("Дома"));
					}
				}
			}
			collationPart.onCalc(collation);
			updateStatus("Групповой прогноз сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	private List<Model> aspects;

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private SkyPointAspect calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим транзитный угол
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExact(res)) {
					SkyPointAspect spa = new SkyPointAspect();
					spa.setScore(res);
					spa.setAspect(a);
					spa.setSkyPoint1(point1);
					spa.setSkyPoint2(point2);
					spa.setExact(true);
					return spa;
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Расчёт транзитов
	 */
	private Map<String, Object> makeTransits(Event event, Event person) {
		Map<String, Object> map = new HashMap<String, Object>();
		List<SkyPointAspect> aspectMap = new ArrayList<>();
		List<SkyPointAspect> dirMap = new ArrayList<>();
		List<PlanetHouseText> houseMap = new ArrayList<>();

		Long[] pfilter = Planet.getSportSet();
		Long[] hfilter = House.getSportSet();

		if (null == person.getConfiguration())
			person.init(false);
		if (null == event.getConfiguration())
			event.init(false);
		List<Model> planets = person.getConfiguration().getPlanets();
		List<Model> planets2 = event.getConfiguration().getPlanets();
		List<Model> houses = event.getConfiguration().getHouses();

		for (Model model : planets) {
			if (!Arrays.asList(pfilter).contains(model.getId()))
				continue;
			Planet planet = (Planet)model;
			//дирекции планеты участника к планетам события
			for (Model model2 : planets2) {
				if (!Arrays.asList(pfilter).contains(model2.getId()))
					continue;
				Planet eplanet = (Planet)model2;
				SkyPointAspect aspect = calc(planet, eplanet);
				if (aspect != null)
					aspectMap.add(aspect);
			}
			//дирекции планеты участника к куспидам домов события
			for (Model model2 : houses) {
				if (!Arrays.asList(hfilter).contains(model2.getId()))
					continue;
				House house = (House)model2;
				SkyPointAspect aspect = calc(planet, house);
				if (aspect != null)
					dirMap.add(aspect);
			}
		}
		//планеты участника в домах события
		for (Model model : planets) {
			Planet planet = (Planet)model;
			for (int j = 0; j < houses.size(); j++) {
				House house = (House)houses.get(j);
				if (!Arrays.asList(hfilter).contains(house.getId()))
					continue;
				double pcoord = planet.getCoord();
				Double hmargin = (j == houses.size() - 1) ?
					((House)houses.get(0)).getCoord() : 
					((House)houses.get(j + 1)).getCoord();
				double[] res = CalcUtil.checkMarginalValues(house.getCoord(), hmargin, pcoord);
				hmargin = res[0];
				pcoord = res[1];
				//если градус планеты находится в пределах куспидов
				//текущей и предыдущей трети домов,
				//запоминаем, в каком доме находится планета
				if (Math.abs(pcoord) < hmargin & 
						Math.abs(pcoord) >= house.getCoord()) {
					PlanetHouseText phouse = new PlanetHouseText();
					phouse.setPlanet(planet);
					phouse.setHouse(house);
					houseMap.add(phouse);
					break;
				}
			}
		}
		map.put("Аспекты", aspectMap);
		map.put("Дирекции", dirMap);
		map.put("Дома", houseMap);
		return map;
	}

	@CanExecute
	public boolean canExecute(@Active MPart activePart) {
		CollationPart collationPart = (CollationPart)activePart.getObject();
		Collation collation = null;
		try {
			collation = (Collation)collationPart.getModel(0, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (collation != null && collation.getId() != null);
	}
}
