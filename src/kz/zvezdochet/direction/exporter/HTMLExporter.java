package kz.zvezdochet.direction.exporter;

import html.Tag;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.service.DirectionService;
import kz.zvezdochet.export.util.HTMLUtil;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * Генератор HTML-файлов для экспорта данных
 * @author Nataly Didenko
 *
 */
@SuppressWarnings("unchecked")
public class HTMLExporter {
	private HTMLUtil util;

	public HTMLExporter() {
		util = new HTMLUtil();
	}

	/**
	 * Генерация событий периода
	 * @param event событие
	 */
	public void generate(Event event, List<SkyPointAspect> spas) {
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
	
			//заголовок
			Tag row = new Tag("tr");
			Tag cell = new Tag("td");
			Tag h1 = new Tag("h3");
			h1.add("Прогноз событий");
			cell.add(h1);
			row.add(cell);
			table.add(row);
			
			//содержание
			Tag ul = new Tag("ol");
			DirectionService service = new DirectionService();
			boolean child = false;
			int age = 0;
			int i = 0;
			for (SkyPointAspect spa : spas) {
				++i;
				int sage = (int)spa.getAge();
				if (age != sage) {
					age = sage;
					child = age < event.MAX_TEEN_AGE;
					if (i > 1) {
						cell.add(ul);
						row.add(cell);
						table.add(row);
					}
					Tag tr = new Tag("tr");
					Tag td = new Tag("td", "class=header id=age" + age);
					td.add(CoreUtil.getAgeString(age));
					tr.add(td);
					table.add(tr);
					row = new Tag("tr");
					cell = new Tag("td");
					ul = new Tag("ul");
				}
				Aspect aspect = spa.getAspect();
				if (aspect.isMain()) {
					AspectType type = aspect.getType();
					Tag li = new Tag("li", "style=color:" + type.getFontColor());
					Planet planet = (Planet)spa.getSkyPoint1();
					SkyPoint skyPoint = spa.getSkyPoint2();
					if (skyPoint instanceof House) {
						House house = (House)skyPoint;
						Tag p = new Tag("p");
						Tag b = new Tag("b");
						b.add(house.getShortName());
						p.add(b);
						li.add(p);

						DirectionText dirText = (DirectionText)service.find(planet, house, type);
						if (dirText != null) {
							li.add(dirText.getText());
							List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
							for (TextGender gender : genders)
								li.add(gender.getText());
						}
					} else if (skyPoint instanceof Planet) {
						Planet planet2 = (Planet)skyPoint;
						Tag p = new Tag("p");
						Tag b = new Tag("b");
						b.add(planet.getName() + " " + type.getSymbol() + " " + planet2.getName());
						p.add(b);
						li.add(p);

//						DirectionText dirText = (DirectionText)service.find(planet, house, type);
//						if (dirText != null) {
//							li.add(dirText.getText());
//							List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
//							for (TextGender gender : genders)
//								li.add(gender.getText());
//						}
					}
					ul.add(li);
				}
				if (i == spas.size()) {
					cell.add(ul);
					row.add(cell);
					table.add(row);
				}
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
}
