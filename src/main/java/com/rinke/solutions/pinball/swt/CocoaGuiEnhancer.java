package com.rinke.solutions.pinball.swt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.jface.action.IAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;

/**
 * Provide a hook to connecting the Preferences, About and Quit menu items of
 * the Mac OS X Application menu when using the SWT Cocoa bindings.
 * <p>
 * This code does not require the Cocoa SWT JAR in order to be compiled as it
 * uses reflection to access the Cocoa specific API methods. It does, however,
 * depend on JFace (for IAction), but you could easily modify the code to use
 * SWT Listeners instead in order to use this class in SWT only applications.
 * </p>
 * <p>
 * This code was influenced by the <a href=
 * "http://www.simidude.com/blog/2008/macify-a-swt-application-in-a-cross-platform-way/"
 * >CarbonUIEnhancer from Agynami</a> with the implementation being modified
 * from the <a href=
 * "http://dev.eclipse.org/viewcvs/index.cgi/org.eclipse.ui.cocoa/src/org/eclipse/ui/internal/cocoa/CocoaUIEnhancer.java"
 * >org.eclipse.ui.internal.cocoa.CocoaUIEnhancer</a>.
 * </p>
 */
public class CocoaGuiEnhancer {

	static Callback proc3Args;

	static long sel_aboutMenuItemSelected_;
	static long sel_preferencesMenuItemSelected_;
	static long sel_toolbarButtonClicked_;

	private static final int kAboutMenuItem = 0;
	private static final int kPreferencesMenuItem = 2;
	private static final int kQuitMenuItem = 10;
	private static final int kHideAppMenuItem = 6;
	private static final int kServicesMenuItem = 4;
	private static final int kHideOthersMenuItem = 7;
	private static final int kShowAllMenuItem = 8;

	private static Object invoke(Class<?> clazz, Object target,
			String methodName, Object[] args) {
		try {
			Class<?>[] signature = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) {
				Class<?> thisClass = args[i].getClass();
				if (thisClass == Integer.class)
					signature[i] = int.class;
				else if (thisClass == Long.class)
					signature[i] = long.class;
				else if (thisClass == Byte.class)
					signature[i] = byte.class;
				else if (thisClass == Boolean.class)
					signature[i] = boolean.class;
				else
					signature[i] = thisClass;
			}
			Method method = clazz.getMethod(methodName, signature);
			return method.invoke(target, args);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static Object invoke(Class<?> clazz, String methodName,
			Object[] args) {
		return invoke(clazz, null, methodName, args);
	}

	private static Object wrapPointer(long value) {
		Class<?> PTR_CLASS = C.PTR_SIZEOF == 8 ? long.class : int.class;
		if (PTR_CLASS == long.class)
			return Long.valueOf(value);
		else
			return Integer.valueOf((int) value);
	}

	final private String appName;

	/**
	 * Construct a new CocoaUIEnhancer.
	 * 
	 * @param appName
	 *            The name of the application. It will be used to customize the
	 *            About and Quit menu items. If you do not wish to customize the
	 *            About and Quit menu items, just pass <code>null</code> here.
	 */
	public CocoaGuiEnhancer(String appName) {
		this.appName = appName;
	}

	/**
	 * Hook the given Listener to the Mac OS X application Quit menu and the
	 * IActions to the About and Preferences menus.
	 * 
	 * @param display
	 *            The Display to use.
	 * @param quitListener
	 *            The listener to invoke when the Quit menu is invoked. This
	 *            cannot be <code>null</code> or the SWT classes will complain.
	 * @param aboutAction
	 *            The action to run when the About menu is invoked.
	 * @param preferencesAction
	 *            The action to run when the Preferences menu is invoked.
	 */
	public void hookApplicationMenu(Display display, Listener quitListener,
			final IAction aboutAction, final IAction preferencesAction) {
		// This is our callbackObject whose 'actionProc' method will be called
		// when
		// the About or Preferences menuItem is invoked.

		// Connect the given IAction objects to the actionProce method.
		Object target = new Object() {
			@SuppressWarnings("unused")
			int actionProc(int id, int sel, int arg0) {
				// Casts the parameters to long so and use the method for 64bit
				// cocoa.
				return (int) actionProc((long) id, (long) sel, (long) arg0);
			}

			long actionProc(long id, long sel, long arg0) {
				if (sel == sel_aboutMenuItemSelected_)
					aboutAction.run();
				else if (sel == sel_preferencesMenuItemSelected_)
					preferencesAction.run();

				return 99;
			}
		};

		try {
			// Initialize the menuItems.
			initialize(target);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}

		// Connect the quit/exit menu.
		if (!display.isDisposed()) {
			display.addListener(SWT.Close, quitListener);
		}

		// Schedule disposal of callback object
		display.disposeExec(new Runnable() {
			public void run() {
				invoke(proc3Args, "dispose"); //$NON-NLS-1$
			}
		});
	}

	private Class<?> classForName(String classname) {
		try {
			Class<?> cls = Class.forName(classname);
			return cls;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException(e);
		}
	}

	private long convertToLong(Object object) {
		if (object instanceof Integer) {
			Integer i = (Integer) object;
			return i.longValue();
		}
		if (object instanceof Long) {
			Long l = (Long) object;
			return l.longValue();
		}
		return 0;
	}

	private void initialize(Object callbackObject) throws Exception {

		Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS"); //$NON-NLS-1$

		// Register names in objective-c.
		if (sel_toolbarButtonClicked_ == 0) {
			sel_preferencesMenuItemSelected_ = registerName(osCls,
					"preferencesMenuItemSelected:"); //$NON-NLS-1$
			sel_aboutMenuItemSelected_ = registerName(osCls,
					"aboutMenuItemSelected:"); //$NON-NLS-1$
		}

		// Create an SWT Callback object that will invoke the actionProc method
		// of
		// our internal callbackObject.
		proc3Args = new Callback(callbackObject, "actionProc", 3); //$NON-NLS-1$
		Method getAddress = Callback.class
				.getMethod("getAddress", new Class[0]); //$NON-NLS-1$
		Object object = getAddress.invoke(proc3Args, (Object[]) null);
		long proc3 = convertToLong(object);
		if (proc3 == 0) {
			SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
		}

		Class<?> nsmenuCls = classForName("org.eclipse.swt.internal.cocoa.NSMenu"); //$NON-NLS-1$
		Class<?> nsmenuitemCls = classForName("org.eclipse.swt.internal.cocoa.NSMenuItem"); //$NON-NLS-1$
		Class<?> nsstringCls = classForName("org.eclipse.swt.internal.cocoa.NSString"); //$NON-NLS-1$
		Class<?> nsapplicationCls = classForName("org.eclipse.swt.internal.cocoa.NSApplication"); //$NON-NLS-1$

		// Instead of creating a new delegate class in objective-c,
		// just use the current SWTApplicationDelegate. An instance of this
		// is a field of the Cocoa Display object and is already the target
		// for the menuItems. So just get this class and add the new methods
		// to it.
		object = invoke(osCls, "objc_lookUpClass", //$NON-NLS-1$
				new Object[] { "SWTApplicationDelegate" }); //$NON-NLS-1$
		long cls = convertToLong(object);

		// Add the action callbacks for Preferences and About menu items.
		invoke(osCls, "class_addMethod", new Object[] { wrapPointer(cls), //$NON-NLS-1$
				wrapPointer(sel_preferencesMenuItemSelected_),
				wrapPointer(proc3), "@:@" }); //$NON-NLS-1$
		invoke(osCls, "class_addMethod", new Object[] { wrapPointer(cls), //$NON-NLS-1$
				wrapPointer(sel_aboutMenuItemSelected_), wrapPointer(proc3),
				"@:@" }); //$NON-NLS-1$

		// Get the Mac OS X Application menu.
		Object sharedApplication = invoke(nsapplicationCls, "sharedApplication"); //$NON-NLS-1$
		Object mainMenu = invoke(sharedApplication, "mainMenu"); //$NON-NLS-1$
		Object mainMenuItem = invoke(nsmenuCls, mainMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(0) });
		Object appMenu = invoke(mainMenuItem, "submenu"); //$NON-NLS-1$

		// Create the About <application-name> menu command
		Object aboutMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(kAboutMenuItem) });
		if (appName != null) {
			Object nsStr = invoke(nsstringCls,
					"stringWith", new Object[] { "Über " + appName //$NON-NLS-1$ //$NON-NLS-2$
					});
			invoke(nsmenuitemCls, aboutMenuItem,
					"setTitle", new Object[] { nsStr }); //$NON-NLS-1$

			Object quitMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
					new Object[] { wrapPointer(kQuitMenuItem) });
			nsStr = invoke(nsstringCls,
					"stringWith", new Object[] { appName + " beenden" //$NON-NLS-1$ //$NON-NLS-2$
					});
			invoke(nsmenuitemCls, quitMenuItem,
					"setTitle", new Object[] { nsStr }); //$NON-NLS-1$
		}

		// Set MainMenu Title to appName
		Object nsStr = invoke(nsstringCls, "stringWith", new Object[] { appName //$NON-NLS-1$ //$NON-NLS-2$
				});
		invoke(nsmenuCls, appMenu, "setTitle", new Object[] { nsStr }); //$NON-NLS-1$

		// Enable the Preferences menuItem.
		nsStr = invoke(nsstringCls,
				"stringWith", new Object[] { "Einstellungen ..." //$NON-NLS-1$ //$NON-NLS-2$
				});
		Object prefMenuItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(kPreferencesMenuItem) });
		invoke(nsmenuitemCls, prefMenuItem, "setEnabled", new Object[] { true }); //$NON-NLS-1$
		invoke(nsmenuitemCls, prefMenuItem, "setTitle", new Object[] { nsStr }); //$NON-NLS-1$

		// Set Title of Services
		nsStr = invoke(nsstringCls, "stringWith", new Object[] { "Dienste" //$NON-NLS-1$ //$NON-NLS-2$
				});
		Object prefServiceItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(kServicesMenuItem) });
		invoke(nsmenuitemCls, prefServiceItem,
				"setTitle", new Object[] { nsStr }); //$NON-NLS-1$
		invoke(nsmenuitemCls, prefServiceItem,
				"setEnabled", new Object[] { false }); //$NON-NLS-1$

		// Set Title of Hide <application-name> menu command
		nsStr = invoke(nsstringCls,
				"stringWith", new Object[] { appName + " ausblenden" //$NON-NLS-1$ //$NON-NLS-2$
				});
		Object prefHideItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(kHideAppMenuItem) });
		invoke(nsmenuitemCls, prefHideItem, "setTitle", new Object[] { nsStr }); //$NON-NLS-1$

		// Set Title of Hide others
		nsStr = invoke(nsstringCls,
				"stringWith", new Object[] { "Andere ausblenden" //$NON-NLS-1$ //$NON-NLS-2$
				});
		Object prefHideOthersItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(kHideOthersMenuItem) });
		invoke(nsmenuitemCls, prefHideOthersItem,
				"setTitle", new Object[] { nsStr }); //$NON-NLS-1$

		// Set Title of Show All
		nsStr = invoke(nsstringCls,
				"stringWith", new Object[] { "Alle einblenden" //$NON-NLS-1$ //$NON-NLS-2$
				});
		Object prefShowAllItem = invoke(nsmenuCls, appMenu, "itemAtIndex", //$NON-NLS-1$
				new Object[] { wrapPointer(kShowAllMenuItem) });
		invoke(nsmenuitemCls, prefShowAllItem,
				"setTitle", new Object[] { nsStr }); //$NON-NLS-1$
		// Set the action to execute when the About or Preferences menuItem is
		// invoked.
		//
		// We don't need to set the target here as the current target is the
		// SWTApplicationDelegate
		// and we have registerd the new selectors on it. So just set the new
		// action
		// to invoke the
		// selector.
		invoke(nsmenuitemCls, prefMenuItem, "setAction", //$NON-NLS-1$
				new Object[] { wrapPointer(sel_preferencesMenuItemSelected_) });
		invoke(nsmenuitemCls, aboutMenuItem, "setAction", //$NON-NLS-1$
				new Object[] { wrapPointer(sel_aboutMenuItemSelected_) });
	}

	private Object invoke(Class<?> cls, String methodName) {
		return invoke(cls, methodName, (Class<?>[]) null, (Object[]) null);
	}

	private Object invoke(Class<?> cls, String methodName,
			Class<?>[] paramTypes, Object... arguments) {
		try {
			Method m = cls.getDeclaredMethod(methodName, paramTypes);
			return m.invoke(null, arguments);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private Object invoke(Object obj, String methodName) {
		return invoke(obj, methodName, (Class<?>[]) null, (Object[]) null);
	}

	private Object invoke(Object obj, String methodName, Class<?>[] paramTypes,
			Object... arguments) {
		try {
			Method m = obj.getClass().getDeclaredMethod(methodName, paramTypes);
			return m.invoke(obj, arguments);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private long registerName(Class<?> osCls, String name)
			throws IllegalArgumentException, SecurityException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException {
		Object object = invoke(osCls, "sel_registerName", new Object[] { name }); //$NON-NLS-1$
		return convertToLong(object);
	}
}