package kz.zvezdochet.direction.handler;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.bean.PrintDirection;
import kz.zvezdochet.direction.part.DirectionsPart;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.util.PDFUtil;

/**
 * Сохранение в файл положительных дирекций планет по домам (только соединения).
 * Производится для составления вопросов в рамках ректификации
 * @author Natalie Didenko
 */
public class DirectionsSaveHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			DirectionsPart eventPart = (DirectionsPart)activePart.getObject();
			Event event = eventPart.getEvent();
			if (null == event.getHouses()) return; //TODO выдавать сообщение
			int age = event.getAge();
			updateStatus("Сохранение дирекций в файл", false);

			Collection<Planet> planets = event.getPlanets().values();
			Collection<House> houses = event.getHouses().values();

			//формируем списки дирекций по возрасту
			Map<Integer, List<PrintDirection>> map = new HashMap<Integer, List<PrintDirection>>();
			for (Planet planet : planets) {
				if (planet.getCode().equals("Kethu")) continue;
				double one = planet.getLongitude();
				for (House house : houses) {
					double two = house.getLongitude();
					double res = 0;
					if (two >= one) {
						if (two - one < 189)
							res = two - one;
						else continue;
							//res = 360 - two + one;
						int intres = (int)res;
						if (intres <= age) {
							List<PrintDirection> list = map.get(intres);
							if (null == list)
								list = new ArrayList<PrintDirection>();
							list.add(new PrintDirection(planet, house, age));
							map.put(intres, list);
						}
					}
				}
			}

			//сортируем дирекции по возрасту и формируем толкования
			StringBuffer data = new StringBuffer();
			SortedSet<Integer> keys = new TreeSet<Integer>(map.keySet());
			DirectionService service = new DirectionService();
			int i = 0;
			for (Integer key : keys) {
				List<PrintDirection> list = map.get(key);
				boolean child = key < event.MAX_TEEN_AGE;
				for (PrintDirection dir : list) {
					Planet planet = (Planet)dir.getSkyPoint1();
					House house = (House)dir.getSkyPoint2();
					DirectionText dirText = (DirectionText)service.find(planet, house, null);
					String sign = planet.isDamaged() || planet.isLilithed() ? "-" : "×";
					String row = ++i + ") " + CoreUtil.getAgeString(key) + " [" + planet.getShortName() + " " + sign + " " + house.getName() + "] - ";
					if (null == dirText)
						row += "\n\n";
					else {
						row += PDFUtil.removeTags(dirText.getText(), null) + "\n\n";
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							row += PDFUtil.removeTags(gender.getText(), null) + "\n\n";
					}
					data.append(row);
				}
			}
			updateStatus("Расчёт дирекций завершён", false);

			String datafile = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/questions.txt").getPath(); //$NON-NLS-1$
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter( 
				new FileOutputStream(datafile), "UTF-8"));
			writer.append(data);
			writer.close();
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
			updateStatus("Файл дирекций сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
