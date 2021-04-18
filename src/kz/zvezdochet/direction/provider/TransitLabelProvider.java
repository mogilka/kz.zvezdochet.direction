package kz.zvezdochet.direction.provider;

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.ui.ArrayLabelProvider;

/**
 * Формат таблицы аспектов
 * @author Natalie Didenko
 */
public class TransitLabelProvider extends ArrayLabelProvider implements ITableLabelProvider {

	@Override
	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof SkyPointAspect))
			return null;
		SkyPointAspect aspect = (SkyPointAspect)element;
		Aspect realasp = aspect.getAspect();
		switch (columnIndex) {
			case 0: return String.valueOf(aspect.getAge());
			case 1: return aspect.getSkyPoint1().getName();
			case 2: return (null == realasp) ? "" : realasp.getName();
			case 3: return aspect.getSkyPoint2().getName();
			case 4: return aspect.isRetro() ? "R" : "";
			case 5: return String.valueOf(aspect.getScore());
			case 6: return aspect.getSkyPoint1().getSign() != null ? aspect.getSkyPoint1().getSign().getName() : null;
			case 7: return aspect.getSkyPoint1().getHouse() != null ? aspect.getSkyPoint1().getHouse().getName() : null;
			case 8: return aspect.getDescr();
		}
		return null;
	}
	@Override
	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof SkyPointAspect))
			return null;
		SkyPointAspect aspect = (SkyPointAspect)element;
		switch (columnIndex) {
			case 1: return aspect.getSkyPoint1() instanceof Planet
					? ((Planet)aspect.getSkyPoint1()).getImage() : null;
			case 3: return aspect.getSkyPoint2() instanceof Planet
					? ((Planet)aspect.getSkyPoint2()).getImage() : null;
		}
		return null;
	}
	@Override
	public Color getForeground(Object element, int columnIndex) {
		if (!(element instanceof SkyPointAspect))
			return null;
		if (2 == columnIndex) {
			SkyPointAspect aspect = (SkyPointAspect)element;
			if (aspect.getAspect() != null)
				return aspect.getAspect().getType().getColor();
		}
		return Display.getDefault().getSystemColor(SWT.COLOR_BLACK);
	}
}
