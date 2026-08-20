# 🔧 QUICK FIX GUIDE - MainWindow.java

## The Problem
MainWindow.java is missing imports and variable declarations.

## The Fix (5 minutes)

### Step 1: Add Missing Imports
Add these after line 6 in MainWindow.java:
```java
import com.zerog.network.stellarforge.model.ServerProfile;
import com.zerog.network.stellarforge.utils.FirstRunDetector;
import com.zerog.network.stellarforge.utils.ProfileManager;
import com.zerog.network.stellarforge.gui.components.ProfileListCellRenderer;
import java.util.List;
```

### Step 2: Add Missing Variables
Add these after line 21 in MainWindow.java:
```java
private ServerProfile currentProfile;
private JComboBox<ServerProfile> profileSelector;
private JLabel profileStatusLabel;
```

## Complete Fixed Import Section
Replace lines 1-13 with:
```java
package com.zerog.network.stellarforge.gui;

import com.zerog.network.stellarforge.config.SecureConfig;
import com.zerog.network.stellarforge.model.ServerConfig;
import com.zerog.network.stellarforge.model.ServerProfile;
import com.zerog.network.stellarforge.utils.FirstRunDetector;
import com.zerog.network.stellarforge.utils.ProfileManager;
import com.zerog.network.stellarforge.gui.components.ProfileListCellRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
```

## Complete Fixed Variables Section
Replace lines 18-22 with:
```java
public class MainWindow extends JFrame {
    private static final Logger logger = LoggerFactory.getLogger(MainWindow.class);
    
    private ServerConfig serverConfig;
    private ServerProfile currentProfile;
    private JComboBox<ServerProfile> profileSelector;
    private JLabel profileStatusLabel;
```

## Test the Fix
```powershell
cd "D:\ADriveJava\Java Application Development\StellarServerForge"
mvn compile
```

**Expected:** Should compile with 0 errors!

---

## Alternative: Use find-replace in your IDE

1. Open MainWindow.java
2. Find line 1-14 (imports section)
3. Replace with the complete fixed import section above
4. Find line 18-22 (class variables)
5. Replace with the complete fixed variables section above
6. Save
7. Run `mvn compile`

---

That's it! These simple additions will fix all compilation errors.


