package kz.zvezdochet.direction.handler;

import java.util.List;

import javax.inject.Inject;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.HousePart;
import kz.zvezdochet.part.EventPart;
import kz.zvezdochet.util.Configuration;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

/**
 * Расчёт дирекций планет по домам
 * @author Nataly Didenko
 */
public class HouseHandler extends Handler {
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

			List<Model> planets = conf.getPlanets();
			List<Model> houses = conf.getHouses();
			int hcount = houses.size();
			int pcount = planets.size();
			String[][] data = new String[hcount][pcount + 1];
			//заполняем заголовки строк названиями куспидов и третей домов и их координатами
			for (int i = 0; i < hcount; i++) {
				House house = (House)houses.get(i);
				data[i][0] = house.getName() + " (" + CalcUtil.roundTo(house.getCoord(), 1) + ")";
			}

			//формируем массив дирекций планет по домам
			for (int c = 0; c < pcount; c++) {
				Planet planet = (Planet)planets.get(c);
				for (int r = 0; r < hcount; r++) {
					House house = (House)houses.get(r);
					double one = Math.abs(planet.getCoord());
					double two = Math.abs(house.getCoord());
					double res;
					if (one - two > 0)
						if (one - two < 189)
							res = one - two;
						else
							res = 360 - one + two;
					else if (two - one < 189)
							res = two - one;
						else
							res = 360 - two + one;
					if (res < 100) //TODO корректировать лимит возраста по дате смерти? =)
						data[r][c + 1] = String.valueOf(CalcUtil.roundTo(Math.abs(res), 1));
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.houses");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    HousePart housePart = (HousePart)part.getObject();
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
