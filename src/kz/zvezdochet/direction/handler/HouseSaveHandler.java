package kz.zvezdochet.direction.handler;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
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
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.core.util.StringUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.bean.PrintDirection;
import kz.zvezdochet.direction.part.HousePart;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.util.Configuration;

/**
 * Сохранение в файл положительных дирекций планет по домам (только соединения).
 * Производится для составления вопросов в рамках ректификации
 * @author Nataly Didenko
 */
public class HouseSaveHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			HousePart eventPart = (HousePart)activePart.getObject();
			Configuration conf = eventPart.getConfiguration();
			if (null == conf) return; //TODO выдавать сообщение
			if (null == conf.getHouses()) return; //TODO выдавать сообщение
			Event event = conf.getEvent();
			int age = event.getAge();
			updateStatus("Сохранение дирекций в файл", false);

			List<Model> planets = conf.getPlanets();
			List<Model> houses = conf.getHouses();
			int hcount = houses.size();
			int pcount = planets.size();

			//формируем списки дирекций по возрасту
			Map<Integer, List<PrintDirection>> map = new HashMap<Integer, List<PrintDirection>>();
			for (int c = 0; c < pcount; c++) {
				Planet planet = (Planet)planets.get(c);
				if (planet.getCode().equals("Kethu")) continue;
				double one = Math.abs(planet.getCoord());
				for (int r = 0; r < hcount; r++) {
					House house = (House)houses.get(r);
					double two = Math.abs(house.getCoord());
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
						row += StringUtil.removeTags(dirText.getText()) + "\n\n";
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							row += StringUtil.removeTags(gender.getText()) + "\n\n";
					}
					data.append(row);
				}
			}
			updateStatus("Расчёт дирекций завершён", false);

			String datafile = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/questions.txt").getPath(); //$NON-NLS-1$
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter( 
				new FileOutputStream(datafile), "UTF-8"));
			String text = "Добрый день, " + event.getName() + ".\n\n"
				+ "Предварительный расчёт сделан, нужно его уточнить."
				+ " Для этого по каждому пункту, приведённому ниже, ответьте, происходили ли в вашей жизни описанные события.\n\n"
				+ "Если происходили, укажите точный возраст или дату.\n"
				+ "Если происходили ранее или позднее указанного возраста, укажите правильный возраст или дату.\n"
				+ "Если не происходили или не помните, так и пишите.\n\n"
				+ "Формулировки не принимайте буквально, это скорее ассоциации, где верным может оказаться один вариант или все варианты.\n\n"
				+ "По событиям, которые совпали, добавьте пару слов, это мне поможет.\n\n";
			writer.append(text);
			writer.append(data);
			writer.close();
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
			updateStatus("Файл дирекций сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
