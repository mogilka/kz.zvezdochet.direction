 
package kz.zvezdochet.direction.handler;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.TextGender;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CoreUtil;
import kz.zvezdochet.core.util.PlatformUtil;
import kz.zvezdochet.direction.Activator;
import kz.zvezdochet.direction.bean.DirectionText;
import kz.zvezdochet.direction.part.AgePart;
import kz.zvezdochet.direction.service.DirectionService;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Сохранение событий периода в файл
 * @author Nataly Didenko
 *
 */
public class AgeSaveHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			AgePart agePart = (AgePart)activePart.getObject();
			@SuppressWarnings("unchecked")
			List<SkyPointAspect> spas = (List<SkyPointAspect>)agePart.getData();
			if (null == spas) return;
			Event event = agePart.getEvent();
			updateStatus("Сохранение дирекций в файл", false);

			StringBuffer data = new StringBuffer();
			DirectionService service = new DirectionService();
			int i = 0;
			for (SkyPointAspect spa : spas) {
				int age = (int)spa.getAge();
				boolean child = age < event.MAX_TEEN_AGE;
				Planet planet = (Planet)spa.getSkyPoint1();
				if (spa.getSkyPoint2() instanceof House) {
					House house = (House)spa.getSkyPoint2();
					DirectionText dirText = (DirectionText)service.find(planet, house, null);
					String row = ++i + ") " + CoreUtil.getAgeString(age) + " [" + planet.getName() + " " + house.getCombination() + "] - ";
					if (null == dirText)
						row += "\n\n";
					else {
						row += dirText.getText() + "\n\n";
						List<TextGender> genders = dirText.getGenderTexts(event.isFemale(), child);
						for (TextGender gender : genders)
							row += gender.getText() + "\n\n";
					}
					data.append(row);
				}
			}
			updateStatus("Сохранение дирекций завершено", false);

			String datafile = PlatformUtil.getPath(Activator.PLUGIN_ID, "/out/events.txt").getPath(); //$NON-NLS-1$
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(datafile), "UTF-8"));
			String text = "Ваш прогноз на желаемый период:\n\n";
			writer.append(text);
			writer.append(data);
			writer.close();
			//TODO показывать диалог, что документ сформирован
			//а ещё лучше открывать его
			updateStatus("Файл событий сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
