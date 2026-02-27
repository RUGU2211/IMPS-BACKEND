# Remove Cursor-style long filenames

If any diagram file was saved with an auto-generated name (e.g. from pasting an image in Cursor), it will look like:

- `c__Users_rugve_AppData_Roaming_Cursor_User_workspaceStorage_9b57cd67..._images_3fc6be1c-af25-4bc1-....png`

**What to do:**

1. **Rename** the file to the correct report name (e.g. `01_System_Architecture.png`) if it is the diagram you want to keep.
2. **Delete** the file if it is a duplicate or not needed.

**PowerShell (run from docs/diagrams):**  
To list such files (no delete):

```powershell
Get-ChildItem . -File | Where-Object { $_.Name -match "workspaceStorage|c__Users|AppData_Roaming" } | Select-Object Name
```

To delete them (use only if you are sure):

```powershell
Get-ChildItem . -File | Where-Object { $_.Name -match "workspaceStorage|c__Users|AppData_Roaming" } | Remove-Item -Force
```

Keep only the official filenames: `01_System_Architecture.png` … `07_Services_Ports_Table.png`, `FULL_PROJECT_ARCHITECTURE.png`, and the other named diagrams in README.txt.
