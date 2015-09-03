package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import kz.zvezdochet.direction.part.DatePart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.util.Configuration;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик расчёта транзитов на указанную дату
 * @author Nataly Didenko
 */
public class DateCalcHandler extends Handler {
	private List<SkyPointAspect> aged = null;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			aged = new ArrayList<SkyPointAspect>();
			DatePart datePart = (DatePart)activePart.getObject();
			if (!datePart.check(0)) return;
			Event person = datePart.getPerson();

			Configuration conf = person.getConfiguration();
			List<Model> planets = conf.getPlanets();
			List<Model> houses = conf.getHouses();
			
			updateStatus("Расчёт транзитов на указанную дату", false);
			Date seldate = datePart.getDate();

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

			//
//			double avey = ((366 * leaps) + (365 * (years - leaps)));
			double age = days / 365.25;

			//дирекции планеты к другим планетам
			List<Model> trplanets = new ArrayList<Model>();
			for (Model model: planets)
				trplanets.add(new Planet((Planet)model));
			for (Model model : trplanets) {
				Planet trplanet = (Planet)model;
				double coord = CalcUtil.getAgedCoord(Math.abs(trplanet.getCoord()), age);
				trplanet.setCoord(coord);
				for (Model model2 : planets) {
					Planet planet = (Planet)model2;
					calc(trplanet, planet, age);
				}
			}

			//дирекции планеты к куспидам домов
			for (Model model : trplanets) {
				Planet trplanet = (Planet)model;
				for (Model model2 : houses) {
					House house = (House)model2;
					calc(trplanet, house, age);
				}
			}
			updateStatus("Расчёт транзитов завершён", false);
		    datePart.setData(aged);
			updateStatus("Таблица транзитов сформирована", false);

			Event event = new Event();
			Configuration conf2 = new Configuration(seldate);
			conf2.setPlanets(trplanets);
			event.setConfiguration(conf2);
			datePart.setEvent(event);
			datePart.onCalc(0);
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
	 * @param age возраст
	 * @param retro признак попятного движения
	 */
	private void calc(SkyPoint point1, SkyPoint point2, double age) {
		try {
			//находим угол между точками космограммы с учетом возраста
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			List<Model> aspects = null;
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExactTruncAspect(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAge(age);
					aspect.setAspect(a);
					aged.add(aspect);
				}
			}
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber() + ", " + age);
			e.printStackTrace();
		}
	}
}
