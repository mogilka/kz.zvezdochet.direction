package kz.zvezdochet.direction.exporter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import html.Tag;
import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.service.DirectionAspectService;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.service.AspectTypeService;

/**
 * Генератор HTML-файлов для экспорта данных
 * @author Natalie Didenko
 *
 */
@SuppressWarnings("unchecked")
public class HTMLExporter {

	/**
	 * Генерация событий периода
	 * @param event событие
	 */
	public void generate(Event event, List<SkyPointAspect> spas, int initage, int finalage) {
		try {
			Tag html = new Tag("html");
			Tag head = new Tag("head");
			head.add(new Tag("meta", "http-equiv=Content-Type content=text/html; charset=UTF-8"));
			head.add(new Tag("link", "href=horoscope_files/horoscope.css rel=stylesheet type=text/css"));
			Tag title = new Tag("title");
			title.add("Прогноз событий");
			head.add(title);
			html.add(head);

			Tag body = new Tag("body");
			body.add(printCopyright());
			Tag table = new Tag("table");
			body.add(table);
			body.add(printCopyright());
			html.add(body);

			//дата события
			Tag row = new Tag("tr");
			Tag cell = new Tag("td", "class=mainheader");
			Place place = event.getPlace();
			if (null == place)
				place = new Place().getDefault();
			cell.add(DateUtil.fulldtf.format(event.getBirth()) +
				"&ensp;" + (event.getZone() >= 0 ? "UTC+" : "") + event.getZone() +
				"&ensp;" + (event.getDst() >= 0 ? "DST+" : "") + event.getDst() + 
				"&emsp;" + place.getName() +
				"&ensp;" + place.getLatitude() + "&#176;" +
				", " + place.getLongitude() + "&#176;");
			row.add(cell);
			table.add(row);
			
			//содержание
			row = new Tag("tr");
			cell = new Tag("td");
			generateContents(spas, cell, initage, finalage);
			row.add(cell);
			table.add(row);

			//события
			Map<Integer, Map<String, List<SkyPointAspect>>> map = new HashMap<Integer, Map<String, List<SkyPointAspect>>>();
			for (SkyPointAspect spa : spas) {
				int age = (int)spa.getAge();
				Map<String, List<SkyPointAspect>> agemap = map.get(age);
				if (null == agemap) {
					agemap = new HashMap<String, List<SkyPointAspect>>();
					agemap.put("main", new ArrayList<SkyPointAspect>());
					agemap.put("strong", new ArrayList<SkyPointAspect>());
					agemap.put("inner", new ArrayList<SkyPointAspect>());
				}
				String code = spa.getAspect().getType().getCode();
				if (code.equals("NEUTRAL") || code.equals("NEGATIVE") || code.equals("POSITIVE")) {
					if (spa.getSkyPoint2() instanceof Planet) {
						List<SkyPointAspect> list = agemap.get("inner");
						list.add(spa);
					} else {
						if (code.equals("NEUTRAL")) {
							List<SkyPointAspect> list = agemap.get("main");
							list.add(spa);
						} else {
							List<SkyPointAspect> list = agemap.get("strong");
							list.add(spa);
						}
					}
				}
				map.put(age, agemap);
			}
			for (Map.Entry<Integer, Map<String, List<SkyPointAspect>>> entry : map.entrySet()) {
			    int age = entry.getKey();
			    Map<String, List<SkyPointAspect>> agemap = entry.getValue();
				for (Map.Entry<String, List<SkyPointAspect>> subentry : agemap.entrySet())
					generateEvents(event, table, age, subentry.getKey(), subentry.getValue());
			}
			
			if (html != null) {
//				System.out.println(html);
				export(html.toString());
			}
		} catch(Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Ошибка", e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Экспорт данных в файл
	 * @param html-файл
	 * @todo использовать конфиг для задания пути
	 */
	private void export(String html) {
		try {
			String datafile = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/events.html").getPath(); //$NON-NLS-1$
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter( 
				new FileOutputStream(datafile), "UTF-8"));
			writer.append(html);
			writer.close();
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Экспорт завершён");
		}
	}

	/**
	 * Отображение информации о копирайте
	 * @return html-тег с содержимым
	 */
	private Tag printCopyright() {
		Tag cell = new Tag("div", "class=copyright");
		cell.add("&copy; 1998-");
		Tag script = new Tag("script", "type=text/javascript");
		script.add("var year = new Date().getYear(); if (year < 1000) year += 1900; document.write(year);");
		cell.add(script);
		cell.add("Астрологический сервис" + "&nbsp;");
		Tag a = new Tag("a", "href=http://zvezdochet.guru/ target=_blank");
		a.add("«Звездочёт»");
		cell.add(a);
		cell.add(" &mdash; Взгляни на себя в прошлом, настоящем и будущем");
		return cell;
	}

	/**
	 * Генерация событий по категориям
	 */
	private void generateEvents(Event event, Tag table, int age, String code, List<SkyPointAspect> spas) {
		try {
			Tag row = new Tag("tr");
			String options = "class=header";
			if (code.equals("strong"))
				options += " id=" + age;
			Tag cell = new Tag("td", options);

			String header = "";
			if (code.equals("main"))
				header = "Главные события";
			else if (code.equals("strong"))
				header = "Менее значимые события";
			else if (code.equals("inner"))
				header = "Проявления личности";
			cell.add(CoreUtil.getAgeString(age) + ": " + header);
			row.add(cell);
			table.add(row);

			row = new Tag("tr");
			cell = new Tag("td");

			DirectionService service = new DirectionService();
			DirectionAspectService servicea = new DirectionAspectService();
			AspectTypeService typeService = new AspectTypeService();

			boolean child = age < event.MAX_TEEN_AGE;
			for (SkyPointAspect spa : spas) {
				AspectType type = spa.getAspect().getType();
				String tcode = type.getCode();
				if (tcode.contains("HIDDEN")) {
					if (tcode.contains("NEGATIVE"))
						type = (AspectType)typeService.find("NEGATIVE");
					else if (tcode.contains("POSITIVE"))
						type = (AspectType)typeService.find("POSITIVE");
				}
				Tag h5 = new Tag("h5");

				Planet planet = (Planet)spa.getSkyPoint1();
				SkyPoint skyPoint = spa.getSkyPoint2();
				if (skyPoint instanceof House) {
					House house = (House)skyPoint;
					h5.add(planet.getShortName() + " " + type.getSymbol() + " " + house.getName());
					cell.add(h5);

					DirectionText dirText = (DirectionText)service.find(planet, house, type);
					if (dirText != null) {
						Tag li = new Tag("div", "style=color:" + type.getFontColor());
						li.add(dirText.getText());
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							li.add(gender.getText());
						cell.add(li);
					}
				} else if (skyPoint instanceof Planet) {
					Planet planet2 = (Planet)skyPoint;
					if (planet.getNumber() > planet2.getNumber())
						continue;
					h5.add(planet.getShortName() + " " + type.getSymbol() + " " + planet2.getShortName());
					cell.add(h5);

					PlanetAspectText dirText = (PlanetAspectText)servicea.find(planet, planet2, spa.getAspect());
					if (dirText != null) {
						Tag li = new Tag("div", "style=color:" + type.getFontColor());
						li.add(dirText.getText());
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							li.add(gender.getText());
						cell.add(li);
					}
				}
			}
			row.add(cell);
			table.add(row);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	/**
	 * Генерация содержания
	 * @param spas списоксобытие
	 * @param cell ячейка-контейнер таблицы разметки
	 */
	private void generateContents(List<SkyPointAspect> spas, Tag cell, int initage, int finalage) {
		try {
			Tag p = new Tag("p");
			p.add("Прогноз содержит как позитивные, так и негативные события. Негатив - признак того, что вам необходим отдых и переосмысление. Не зацикливайтесь на негативе, развивайте свои сильные стороны, используя благоприятные события. ");
			p.add("Если из возраста в возраст события повторяются, значит они создают большой резонанс. Максимальная погрешность прогноза события ±1 год.");
			cell.add(p);

			Tag b = new Tag("h5");
			b.add("Возраст:");
			cell.add(b);

			for (int i = initage; i <= finalage; i++) {
				Tag a = new Tag("a", "href=#" + i);
				a.add(CoreUtil.getAgeString(i));
				cell.add("&emsp;");
				cell.add(a);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
