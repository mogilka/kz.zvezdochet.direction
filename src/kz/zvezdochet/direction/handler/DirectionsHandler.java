package kz.zvezdochet.direction.handler;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.DirectionsPart;
import kz.zvezdochet.part.EventPart;
import kz.zvezdochet.util.Configuration;

/**
 * Расчёт дирекций планет по домам
 * @author Nataly Didenko
 */
public class DirectionsHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_CALC, true);
			if (null == event) return;
			Configuration conf = event.getConfiguration();
			if (null == conf) return; //TODO выдавать сообщение
			if (null == conf.getHouses()) return; //TODO выдавать сообщение
			updateStatus("Расчёт дирекций планет по домам", false);

			Collection<Planet> planets = conf.getPlanets().values();
			List<Model> houses = conf.getHouses();
			int hcount = houses.size();
			int pcount = planets.size();
			String[][] data = new String[hcount][pcount + 1];
			//заполняем заголовки строк названиями куспидов и третей домов и их координатами
			for (int i = 0; i < hcount; i++) {
				House house = (House)houses.get(i);
				data[i][0] = house.getName() + " (" + CalcUtil.roundTo(house.getLongitude(), 1) + ")";
			}

			//формируем массив дирекций планет по домам
			for (Planet planet : planets) {
				double one = Math.abs(planet.getLongitude());
				for (int r = 0; r < hcount; r++) {
					House house = (House)houses.get(r);
					double two = Math.abs(house.getLongitude());
					double res;
					boolean retro = false;
					if (one - two > 0) {
						if (one - two < 189) {
							res = one - two;
							retro = true;
						} else
							res = 360 - one + two;
					} else if (two - one < 189)
						res = two - one;
					else {
						res = 360 - two + one;
						retro = true;
					}
					if (retro)
						res *= -1;
					if (Math.abs(res) < 100) //TODO корректировать лимит возраста по дате смерти? =)
						data[r][planet.getId().intValue() - 18] = String.valueOf(CalcUtil.roundTo(res, 2));
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.directions");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    DirectionsPart housePart = (DirectionsPart)part.getObject();
		    housePart.setConfiguration(conf);
		    housePart.setData(data);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
