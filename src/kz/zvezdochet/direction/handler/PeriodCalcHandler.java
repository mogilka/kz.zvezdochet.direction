package kz.zvezdochet.direction.handler;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.direction.part.PeriodPart;
import kz.zvezdochet.service.EventService;
import kz.zvezdochet.util.Configuration;

/**
 * Обработчик расчёта транзитов на указанный период
 * @author Nataly Didenko
 */
public class PeriodCalcHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			PeriodPart periodPart = (PeriodPart)activePart.getObject();
				if (!periodPart.check(0)) return;
			Event person = periodPart.getPerson();
	
			Configuration conf = person.getConfiguration();
			List<Model> planets = conf.getPlanets();
			List<Model> houses = conf.getHouses();
	
			updateStatus("Расчёт транзитов на период", false);
			Planet moon = null;
			for (Model model : planets) {
				Planet planet = (Planet)model;
				if ("Moon" == planet.getCode()) {
					moon = planet;
					break;
				}
			}
	
			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			LocalDate start = initDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			LocalDate end = finalDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			EventService service = new EventService();
			for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
			    List<Model> list = service.findByDate(date., 1);
			}
		
	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}