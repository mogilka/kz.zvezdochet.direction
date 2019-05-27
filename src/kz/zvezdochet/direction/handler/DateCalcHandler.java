package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.part.EventPart;
import kz.zvezdochet.service.AspectService;

/**
 * Обработчик расчёта транзитов на указанную дату.
 * Двигаем натальные планеты от первоначального положения
 * на количество градусов, соответствующих текущему возрасту персоны,
 * и анализируем данные как транзиты
 * @author Natalie Didenko
 */
public class DateCalcHandler extends Handler {
	protected List<SkyPointAspect> aged;
	private List<Model> aspects = null;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			aged = new ArrayList<SkyPointAspect>();
			EventPart transitPart = (EventPart)activePart.getObject();
			if (!transitPart.check(1)) return;
			Event person = transitPart.getPerson();

			Collection<Planet> planets = person.getPlanets().values();
			List<Model> houses = person.getHouses();
			
			updateStatus("Расчёт транзитов на указанную дату", false);
			Date seldate = transitPart.getDate();

			//подсчитываем количество дней от натальной даты до транзитной с учётом високосных годов
			int year1 = person.getBirthYear();
			Calendar calendar = GregorianCalendar.getInstance();
			calendar.setTime(seldate);
			int year2 = calendar.get(Calendar.YEAR);
			boolean leap1 = false, leap2 = false;

			long days = 0;
			for (int y = year1; y <= year2; y++) {
				if (DateUtil.isLeapYear(y)) {
					days += 366;
					if (y == year1)
						leap1 = true;
					if (y == year2)
						leap2 = true;
				} else
					days += 365;
			}

			//вычитаем дни от 1 января натального года до натальной даты
			long difdays = DateUtil.getDateDiff(person.getBirth(), DateUtil.getDate("01.01." + (year1 + 1)), TimeUnit.DAYS);
			int ydays = leap1 ? 366 : 365;
			days -= (ydays - difdays);
			//вычитаем дни от транзитной даты до 1 января года, следующего за транзитным
			difdays = DateUtil.getDateDiff(DateUtil.getDate("01.01." + year2), seldate, TimeUnit.DAYS);
			ydays = leap2 ? 366 : 365;
			days -= (ydays - difdays);
			double age = days / 365.25;

			//увеличиваем координаты планет и домов персоны на значение возраста
			TreeMap<Long, Planet> trplanets = new TreeMap<>();
			for (Planet p: planets) {
				Planet planet = new Planet((Planet)p);
				double coord = CalcUtil.getAgedCoord(Math.abs(planet.getLongitude()), age);
				planet.setLongitude(coord);
				trplanets.put(planet.getId(), planet);
			}
			person.setPlanets(trplanets);

			List<Model> trhouses = new ArrayList<Model>();
			for (Model model: houses) {
				House house = new House((House)model);
				double coord = CalcUtil.getAgedCoord(Math.abs(house.getLongitude()), age);
				house.setLongitude(coord);
				trhouses.add(house);
			}
			person.setHouses(trhouses);

			//инициализируем транзитное событие из выбранной даты и места
			transitPart.resetEvent();
			Event event = transitPart.getModel();

			//инициализируем список аспектов
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			//дирекции планеты к другим планетам и куспидам домов
			for (Planet trplanet : trplanets.values()) {
				for (Planet planet : event.getPlanets().values())
					calc(trplanet, planet);
				for (Model model2 : event.getHouses()) {
					House house = (House)model2;
					calc(trplanet, house);
				}
			}
			updateStatus("Расчёт транзитов завершён", false);
		    transitPart.setTransitData(aged);
			updateStatus("Таблица транзитов сформирована", false);

			transitPart.setModel(event);
			transitPart.onCalc(event, person);
			updateStatus("Космограмма транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private void calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getLongitude(), point2.getLongitude());
	
			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExact(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					aspect.setExact(true);
					aged.add(aspect);
					break;
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}

	/**
	 * Расчёт транзитов
	 * @param person персона
	 * @param event событие
	 */
	protected void makeTransits(Event person, Event event) {
		//дирекции планеты к другим планетам
		Collection<Planet> trplanets = event.getPlanets().values();
		Collection<Planet> pplanets = person.getPlanets().values();
		for (Planet trplanet : trplanets)
			for (Planet planet : pplanets)
				calc(trplanet, planet);

		//дирекции планеты к куспидам домов
		List<Model> phouses = person.getHouses();
		for (Planet trplanet : trplanets) {
			for (Model model2 : phouses) {
				House house = (House)model2;
				calc(trplanet, house);
			}
		}
		updateStatus("Расчёт транзитов завершён", false);
	}
}
